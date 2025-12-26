package com.domotics.smarthome.entities

/**
 * Base contract for every device in the system.
 */
interface DeviceComponent {
    val id: String
    val name: String
    var status: DeviceStatus

    fun subscribe(subscriber: DeviceSubscriber)
    fun unsubscribe(subscriber: DeviceSubscriber)
    fun updateStatus(newStatus: DeviceStatus)
}

/**
 * Describes a device that can be turned on or off.
 */
interface Switchable {
    fun turnOn()
    fun turnOff()
    fun isOn(): Boolean
}

/**
 * Describes a device that can adjust brightness or power level.
 */
interface Dimmable {
    fun setBrightness(level: Int)
    fun getBrightness(): Int
}

/**
 * Describes a device that produces measurable readings.
 */
interface Measurable<T> {
    fun getCurrentValue(): T
    fun updateValue(newValue: T)
}
