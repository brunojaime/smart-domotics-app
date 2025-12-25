package com.domotics.smarthome.entities

/**
 * Represents a lighting device in the domotics system
 */
class Lighting(
    id: String = java.util.UUID.randomUUID().toString(),
    name: String,
    status: DeviceStatus = DeviceStatus.OFFLINE,
    private var brightness: Int = 0,
    private var poweredOn: Boolean = false
) : Device(id, name, status), Switchable, Dimmable {

    init {
        require(brightness in 0..100) { "Brightness must be between 0 and 100" }
    }

    /**
     * Get current brightness level
     */
    override fun getBrightness(): Int = brightness

    /**
     * Set brightness level and notify subscribers
     */
    override fun setBrightness(level: Int) {
        require(level in 0..100) { "Brightness must be between 0 and 100" }
        if (brightness != level) {
            brightness = level
            if (level > 0) {
                poweredOn = true
                if (status != DeviceStatus.ON) {
                    updateStatus(DeviceStatus.ON)
                }
            } else {
                poweredOn = false
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
    fun isLightOn(): Boolean = poweredOn

    override fun isOn(): Boolean = poweredOn

    /**
     * Turn light on with last brightness or default to 100
     */
    override fun turnOn() {
        if (!poweredOn) {
            poweredOn = true
            if (brightness == 0) {
                brightness = 100
            }
            updateStatus(DeviceStatus.ON)
        }
    }

    /**
     * Turn light off
     */
    override fun turnOff() {
        if (poweredOn) {
            poweredOn = false
            brightness = 0
            updateStatus(DeviceStatus.OFF)
        }
    }

    override fun toString(): String {
        return "Lighting(id='$id', name='$name', status=$status, brightness=$brightness, isOn=$poweredOn)"
    }
}
