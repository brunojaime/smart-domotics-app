package com.domotics.smarthome.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.domotics.smarthome.entities.Device
import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting

data class DeviceState(
    val device: Device,
    val isOn: Boolean,
    val brightness: Int,
    val status: DeviceStatus
)

class DeviceViewModel : ViewModel() {
    private val _devices = mutableStateOf<List<DeviceState>>(emptyList())
    val devices = _devices

    fun addDevice(name: String) {
        if (name.isNotBlank()) {
            val newDevice = Lighting(
                name = name,
                status = DeviceStatus.OFF,
                brightness = 0,
                isOn = false
            )
            val deviceState = DeviceState(
                device = newDevice,
                isOn = false,
                brightness = 0,
                status = DeviceStatus.OFF
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

            // Update the state with new values
            _devices.value = _devices.value.map {
                if (it.device.id == device.id) {
                    DeviceState(
                        device = device,
                        isOn = device.isLightOn(),
                        brightness = device.getBrightness(),
                        status = device.status
                    )
                } else {
                    it
                }
            }
        }
    }
}
