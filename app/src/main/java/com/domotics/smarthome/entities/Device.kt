package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Abstract base class for all devices in the domotics system
 * Supports publisher/subscriber pattern for device state changes
 */
abstract class Device(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override var status: DeviceStatus = DeviceStatus.OFFLINE
) : DeviceComponent {
    private val subscribers = mutableListOf<DeviceSubscriber>()

    init {
        require(name.isNotBlank()) { "Device name cannot be blank" }
    }

    /**
     * Subscribe to device state changes
     */
    override fun subscribe(subscriber: DeviceSubscriber) {
        subscribers.add(subscriber)
    }

    /**
     * Unsubscribe from device state changes
     */
    override fun unsubscribe(subscriber: DeviceSubscriber) {
        subscribers.remove(subscriber)
    }

    /**
     * Notify all subscribers of a state change
     */
    protected fun notifySubscribers() {
        subscribers.forEach { it.onDeviceStateChanged(this) }
    }

    /**
     * Update device status and notify subscribers
     */
    override fun updateStatus(newStatus: DeviceStatus) {
        if (status != newStatus) {
            status = newStatus
            notifySubscribers()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * Device status enum
 */
enum class DeviceStatus {
    ON,
    OFF,
    OFFLINE,
    ERROR
}

/**
 * Interface for subscribers to device state changes
 */
interface DeviceSubscriber {
    fun onDeviceStateChanged(device: Device)
}
