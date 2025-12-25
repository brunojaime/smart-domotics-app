package com.domotics.smarthome.entities

/**
 * Represents a sensor device in the domotics system
 */
class Sensor(
    id: String = java.util.UUID.randomUUID().toString(),
    name: String,
    status: DeviceStatus = DeviceStatus.OFFLINE,
    val sensorType: SensorType,
    private var currentValue: Double = 0.0
) : Device(id, name, status), Measurable<Double> {

    /**
     * Get current sensor reading
     */
    override fun getCurrentValue(): Double = currentValue

    /**
     * Update sensor reading and notify subscribers
     */
    override fun updateValue(newValue: Double) {
        if (currentValue != newValue) {
            currentValue = newValue
            notifySubscribers()
        }
    }

    override fun toString(): String {
        return "Sensor(id='$id', name='$name', status=$status, type=$sensorType, value=$currentValue)"
    }
}

/**
 * Types of sensors supported in the system
 */
enum class SensorType {
    TEMPERATURE,
    HUMIDITY,
    MOTION,
    LIGHT,
    DOOR_WINDOW,
    SMOKE,
    CO2,
    PRESSURE,
    WATER_LEAK,
    OTHER
}
