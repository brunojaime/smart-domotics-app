package com.domotics.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domotics.smarthome.data.device.DeviceDiscoveryRepository
import com.domotics.smarthome.data.device.DeviceDiscoveryRepositoryImpl
import com.domotics.smarthome.data.device.DiscoveredDevice
import com.domotics.smarthome.data.device.DiscoveryState
import com.domotics.smarthome.data.device.MockMdnsScanner
import com.domotics.smarthome.data.device.MockSsdpScanner
import com.domotics.smarthome.data.device.MockWifiScanner
import com.domotics.smarthome.data.device.PairingClient
import com.domotics.smarthome.data.device.PairingCredentials
import com.domotics.smarthome.data.device.PairingService
import com.domotics.smarthome.data.device.PairingServiceImpl
import com.domotics.smarthome.data.device.PairingState
import com.domotics.smarthome.entities.Device
import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting
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
    private val discoveryRepository: DeviceDiscoveryRepository = DeviceDiscoveryRepositoryImpl(
        listOf(MockWifiScanner(), MockMdnsScanner(), MockSsdpScanner()),
    ),
    private val pairingService: PairingService = PairingServiceImpl(object : PairingClient {
        override suspend fun sendCredentials(
            device: DiscoveredDevice,
            credentials: PairingCredentials,
        ): Boolean = true
    }),
) : ViewModel() {
    private val _devices = MutableStateFlow<List<DeviceState>>(emptyList())
    val devices: StateFlow<List<DeviceState>> = _devices.asStateFlow()

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _selectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val selectedDevice: StateFlow<DiscoveredDevice?> = _selectedDevice.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _credentials = MutableStateFlow(PairingCredentials())
    val credentials: StateFlow<PairingCredentials> = _credentials.asStateFlow()

    private var discoveryJob: Job? = null

    fun addDevice(name: String) {
        if (name.isNotBlank()) {
            val newDevice = Lighting(
                name = name,
                status = DeviceStatus.OFF,
                brightness = 0,
                isOn = false,
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
        }
    }

    fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            discoveryRepository.discoverDevices().collectLatest { state ->
                _discoveryState.value = state
                if (state is DiscoveryState.Results) {
                    _selectedDevice.value = state.devices.firstOrNull()
                }
            }
        }
    }

    fun resetDiscovery() {
        discoveryJob?.cancel()
        _discoveryState.value = DiscoveryState.Idle
        _selectedDevice.value = null
        _pairingState.value = PairingState.Idle
    }

    fun selectDevice(device: DiscoveredDevice) {
        _selectedDevice.value = device
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
}
