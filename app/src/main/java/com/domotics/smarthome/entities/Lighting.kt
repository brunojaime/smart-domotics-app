package com.domotics.smarthome.entities

/**
 * Represents a lighting device in the domotics system
 */
class Lighting(
    id: String = java.util.UUID.randomUUID().toString(),
    name: String,
    status: DeviceStatus = DeviceStatus.OFFLINE,
    private var brightness: Int = 0,
    private var isOn: Boolean = false
) : Device(id, name, status) {

    init {
        require(brightness in 0..100) { "Brightness must be between 0 and 100" }
    }

    /**
     * Get current brightness level
     */
    fun getBrightness(): Int = brightness

    /**
     * Set brightness level and notify subscribers
     */
    fun setBrightness(level: Int) {
        require(level in 0..100) { "Brightness must be between 0 and 100" }
        if (brightness != level) {
            brightness = level
            if (level > 0) {
                isOn = true
                if (status != DeviceStatus.ON) {
                    updateStatus(DeviceStatus.ON)
                }
            } else {
                isOn = false
                if (status == DeviceStatus.ON) {
                    updateStatus(DeviceStatus.OFF)
                }
            }
            notifySubscribers()
        }
    }

    /**
     * Check if light is on
     */
    fun isLightOn(): Boolean = isOn

    /**
     * Turn light on with last brightness or default to 100
     */
    fun turnOn() {
        if (!isOn) {
            isOn = true
            if (brightness == 0) {
                brightness = 100
            }
            updateStatus(DeviceStatus.ON)
        }
    }

    /**
     * Turn light off
     */
    fun turnOff() {
        if (isOn) {
            isOn = false
            brightness = 0
            updateStatus(DeviceStatus.OFF)
        }
    }

    override fun toString(): String {
        return "Lighting(id='$id', name='$name', status=$status, brightness=$brightness, isOn=$isOn)"
    }
}
