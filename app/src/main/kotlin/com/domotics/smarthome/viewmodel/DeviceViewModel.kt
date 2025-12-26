package com.domotics.smarthome.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.domotics.smarthome.data.auth.SecureTokenStorage
import com.domotics.smarthome.data.auth.TokenProvider
import com.domotics.smarthome.data.device.DeviceDiscoveryRepository
import com.domotics.smarthome.data.device.DeviceDiscoveryRepositoryImpl
import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.DiscoveryState
import com.domotics.smarthome.data.device.MockBluetoothScanner
import com.domotics.smarthome.data.device.MockMdnsScanner
import com.domotics.smarthome.data.device.MockSsdpScanner
import com.domotics.smarthome.data.device.MockWifiScanner
import com.domotics.smarthome.data.device.PairingCredentials
import com.domotics.smarthome.data.device.PairingService
import com.domotics.smarthome.data.device.PairingServiceImpl
import com.domotics.smarthome.data.device.PairingState
import com.domotics.smarthome.data.device.RadioState
import com.domotics.smarthome.data.device.LocalPairingClient
import com.domotics.smarthome.data.mqtt.LightStatePayload
import com.domotics.smarthome.data.mqtt.MqttBrokerRepository
import com.domotics.smarthome.data.mqtt.MqttConnectionState
import com.domotics.smarthome.entities.Device
import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting
import com.domotics.smarthome.provisioning.BluetoothProvisioningStrategy
import com.domotics.smarthome.provisioning.DiscoveryMetadata
import com.domotics.smarthome.provisioning.OnboardingCodeProvisioningStrategy
import com.domotics.smarthome.provisioning.ProvisioningProgress
import com.domotics.smarthome.provisioning.ProvisioningResult
import com.domotics.smarthome.provisioning.ProvisioningStrategy
import com.domotics.smarthome.provisioning.ProvisioningStrategySummary
import com.domotics.smarthome.provisioning.ProvisioningViewState
import com.domotics.smarthome.provisioning.SoftApProvisioningStrategy
import com.domotics.smarthome.provisioning.WifiCredentials
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DeviceState(
    val device: Device,
    val isOn: Boolean,
    val brightness: Int,
    val status: DeviceStatus,
)

class DeviceViewModel(
    /**
     * By default the view model wires in demo scanners that emit simulated devices so the UI can
     * be exercised without hardware. To talk to real devices, pass a repository backed by
     * production `DeviceScanner` implementations instead.
     */
    private val discoveryRepository: DeviceDiscoveryRepository = DeviceDiscoveryRepositoryImpl(
        listOf(MockWifiScanner(), MockMdnsScanner(), MockSsdpScanner(), MockBluetoothScanner()),
    ),
    pairingService: PairingService? = null,
    private val tokenProvider: TokenProvider? = null,
    private val mqttRepository: MqttBrokerRepository = MqttBrokerRepository(tokenProvider = tokenProvider),
    private val startMqttBridgeOnInit: Boolean = true,
) : ViewModel() {
    private val _radioState = MutableStateFlow(RadioState())
    val radioState: StateFlow<RadioState> = _radioState.asStateFlow()

    private val discoveryMetadata = mutableMapOf<String, DiscoveryMetadata>()
    private val pairingService: PairingService = pairingService
        ?: PairingServiceImpl(LocalPairingClient({ discoveryMetadata[it] }, { _radioState.value }))
    private val _devices = MutableStateFlow<List<DeviceState>>(emptyList())
    val devices: StateFlow<List<DeviceState>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _usesSimulatedDiscovery = MutableStateFlow(false)
    val usesSimulatedDiscovery: StateFlow<Boolean> = _usesSimulatedDiscovery.asStateFlow()

    private val _selectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val selectedDevice: StateFlow<DiscoveredDevice?> = _selectedDevice.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _credentials = MutableStateFlow(PairingCredentials())
    val credentials: StateFlow<PairingCredentials> = _credentials.asStateFlow()

    private var discoveryJob: Job? = null

    private val provisioningStrategies: List<ProvisioningStrategy> = listOf(
        SoftApProvisioningStrategy(),
        BluetoothProvisioningStrategy(),
        OnboardingCodeProvisioningStrategy()
    )

    private val _provisioningState = mutableStateOf(ProvisioningViewState())
    val provisioningState = _provisioningState

    private var provisioningJob: Job? = null
    private var lastDiscoveryMetadata: DiscoveryMetadata? = null

    private var mqttStarted = false

    init {
        if (startMqttBridgeOnInit) {
            startMqttBridge()
        }
        updateDiscovery(DiscoveryMetadata())
    }

    fun setWifiEnabled(enabled: Boolean) {
        _radioState.value = _radioState.value.copy(wifiEnabled = enabled)
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        _radioState.value = _radioState.value.copy(bluetoothEnabled = enabled)
    }

    fun startMqttBridge() {
        if (mqttStarted) return
        mqttStarted = true
        viewModelScope.launch {
            runCatching {
                mqttRepository.authenticateAndConnect()
            }.onFailure { error ->
                Log.e("DeviceViewModel", "MQTT authentication/connect failed", error)
                _connectionState.value = MqttConnectionState.Error(error.message)
            }
        }

        viewModelScope.launch {
            mqttRepository.connectionState.collectLatest { state ->
                _connectionState.value = state
            }
        }

        viewModelScope.launch {
            mqttRepository.lightStateMessages().collectLatest { payload ->
                applyRemoteState(payload)
            }
        }
    }

    fun reconnectMqtt() {
        viewModelScope.launch {
            runCatching {
                mqttRepository.authenticateAndConnect()
            }.onFailure { error ->
                Log.e("DeviceViewModel", "MQTT reconnect failed", error)
                _connectionState.value = MqttConnectionState.Error(error.message)
            }
        }
    }

    fun addDevice(name: String) {
        if (name.isNotBlank()) {
            val newDevice = Lighting(
                name = name,
                status = DeviceStatus.OFF,
                brightness = 0,
                poweredOn = false,
            )
            val deviceState = DeviceState(
                device = newDevice,
                isOn = false,
                brightness = 0,
                status = DeviceStatus.OFF,
            )
            _devices.value = _devices.value + deviceState
        }
    }

    fun updateDiscovery(metadata: DiscoveryMetadata) {
        lastDiscoveryMetadata = metadata
        val available = provisioningStrategies.filter { it.supports(metadata) }
        val summaries = available.map { it.toSummary() }
        _provisioningState.value = _provisioningState.value.copy(
            availableStrategies = summaries,
            selectedStrategy = summaries.firstOrNull(),
            progress = null,
            lastResult = null
        )
    }

    fun selectProvisioningStrategy(strategyId: String) {
        val selected = _provisioningState.value.availableStrategies.firstOrNull { it.id == strategyId }
        if (selected != null) {
            _provisioningState.value = _provisioningState.value.copy(
                selectedStrategy = selected,
                lastResult = null
            )
        }
    }

    fun cancelProvisioning() {
        provisioningJob?.cancel()
        provisioningStrategies.forEach { it.cancel() }
        _provisioningState.value = _provisioningState.value.copy(
            progress = null,
            lastResult = ProvisioningResult.Cancelled()
        )
    }

    fun provisionSelectedStrategy(credentials: WifiCredentials) {
        val metadata = lastDiscoveryMetadata ?: return
        val selectedStrategyId = _provisioningState.value.selectedStrategy?.id ?: return
        val strategy = provisioningStrategies.firstOrNull { it.id == selectedStrategyId } ?: return

        provisioningJob?.cancel()
        _provisioningState.value = _provisioningState.value.copy(
            progress = ProvisioningProgress.ConnectingToDeviceAp,
            lastResult = null
        )

        provisioningJob = viewModelScope.launch {
            val result = strategy.provision(metadata, credentials) { progress ->
                _provisioningState.value = _provisioningState.value.copy(progress = progress)
            }

            _provisioningState.value = _provisioningState.value.copy(
                progress = null,
                lastResult = result
            )
        }
    }

    fun removeDevice(deviceState: DeviceState) {
        _devices.value = _devices.value.filter { it.device.id != deviceState.device.id }
    }

    fun toggleDevice(deviceState: DeviceState) {
        val device = deviceState.device
        if (device is Lighting) {
            if (device.isLightOn()) {
                device.turnOff()
            } else {
                device.turnOn()
            }

            _devices.value = _devices.value.map {
                if (it.device.id == device.id) {
                    DeviceState(
                        device = device,
                        isOn = device.isLightOn(),
                        brightness = device.getBrightness(),
                        status = device.status,
                    )
                } else {
                    it
                }
            }

            mqttRepository.publishLightCommand(
                deviceId = device.id,
                isOn = device.isLightOn(),
                brightness = device.getBrightness(),
            )
        }
    }

    fun startDiscovery() {
        val radios = _radioState.value
        if (!radios.wifiEnabled && !radios.bluetoothEnabled) {
            _discoveryState.value = DiscoveryState.Error("Enable Wi‑Fi or Bluetooth to scan for devices")
            return
        }

        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            discoveryRepository.discoverDevices().collectLatest { state ->
                _discoveryState.value = state
                if (state is DiscoveryState.Results) {
                    discoveryMetadata.clear()
                    _usesSimulatedDiscovery.value = state.devices.any { it.metadata.simulated }
                    state.devices.forEach { device ->
                        discoveryMetadata[device.id] = device.metadata
                    }
                    _selectedDevice.value = state.devices.firstOrNull()
                    state.devices.firstOrNull()?.let { device ->
                        updateDiscovery(discoveryMetadata[device.id] ?: DiscoveryMetadata())
                    }
                }
            }
        }
    }

    fun resetDiscovery() {
        discoveryJob?.cancel()
        _discoveryState.value = DiscoveryState.Idle
        _usesSimulatedDiscovery.value = false
        _selectedDevice.value = null
        _pairingState.value = PairingState.Idle
        discoveryMetadata.clear()
    }

    fun selectDevice(device: DiscoveredDevice) {
        _selectedDevice.value = device
        updateDiscovery(discoveryMetadata[device.id] ?: DiscoveryMetadata())
    }

    fun updateCredentials(ssid: String, password: String) {
        val updated = PairingCredentials(ssid = ssid, password = password)
        _credentials.value = updated
    }

    fun pairSelectedDevice(onPaired: (() -> Unit)? = null) {
        val device = _selectedDevice.value ?: return
        _pairingState.value = PairingState.Submitting
        viewModelScope.launch {
            val pairingResult = pairingService.pairDevice(device, _credentials.value)
            _pairingState.value = pairingResult
            if (pairingResult is PairingState.Success) {
                val pairedState = DeviceState(
                    device = pairingResult.device,
                    isOn = false,
                    brightness = 0,
                    status = DeviceStatus.OFF,
                )
                _devices.value = _devices.value + pairedState
                onPaired?.invoke()
            }
        }
    }

    private fun applyRemoteState(payload: LightStatePayload) {
        val existing = _devices.value.firstOrNull { it.device.id == payload.deviceId }
        val lighting = (existing?.device as? Lighting)
            ?: Lighting(id = payload.deviceId, name = existing?.device?.name ?: "Light ${payload.deviceId}")

        val updatedBrightness = payload.brightness.coerceIn(0, 100)
        if (payload.isOn) {
            lighting.turnOn()
            lighting.setBrightness(updatedBrightness)
        } else {
            lighting.setBrightness(0)
            lighting.turnOff()
        }

        val updatedState = DeviceState(
            device = lighting,
            isOn = lighting.isLightOn(),
            brightness = lighting.getBrightness(),
            status = lighting.status,
        )

        val filtered = _devices.value.filterNot { it.device.id == payload.deviceId }
        _devices.value = filtered + updatedState
    }
}

class DeviceViewModelFactory(private val context: Context, private val startOnInit: Boolean = false) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val tokenStorage = SecureTokenStorage(context.applicationContext)
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(
                tokenProvider = tokenStorage,
                startMqttBridgeOnInit = startOnInit,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun ProvisioningStrategy.toSummary(): ProvisioningStrategySummary =
    ProvisioningStrategySummary(
        id = id,
        name = name,
        requiredUserAction = requiredUserAction
    )
