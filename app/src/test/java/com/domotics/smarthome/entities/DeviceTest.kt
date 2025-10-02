package com.domotics.smarthome.entities

import org.junit.Assert.*
import org.junit.Test

class DeviceTest {

    // Concrete implementation for testing abstract Device class
    private class TestDevice(
        id: String = java.util.UUID.randomUUID().toString(),
        name: String,
        status: DeviceStatus = DeviceStatus.OFFLINE
    ) : Device(id, name, status)

    // Mock subscriber for testing publisher/subscriber pattern
    private class MockSubscriber : DeviceSubscriber {
        var notificationCount = 0
        var lastDevice: Device? = null

        override fun onDeviceStateChanged(device: Device) {
            notificationCount++
            lastDevice = device
        }
    }

    @Test
    fun `create device with default status`() {
        val device = TestDevice(name = "Test Device")

        assertNotNull(device.id)
        assertEquals("Test Device", device.name)
        assertEquals(DeviceStatus.OFFLINE, device.status)
    }

    @Test
    fun `create device with custom status`() {
        val device = TestDevice(name = "Online Device", status = DeviceStatus.ON)

        assertEquals(DeviceStatus.ON, device.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        TestDevice(name = "")
    }

    @Test
    fun `update device status notifies subscribers`() {
        val device = TestDevice(name = "Device")
        val subscriber = MockSubscriber()

        device.subscribe(subscriber)
        device.updateStatus(DeviceStatus.ON)

        assertEquals(1, subscriber.notificationCount)
        assertEquals(device, subscriber.lastDevice)
        assertEquals(DeviceStatus.ON, device.status)
    }

    @Test
    fun `update to same status does not notify subscribers`() {
        val device = TestDevice(name = "Device", status = DeviceStatus.ON)
        val subscriber = MockSubscriber()

        device.subscribe(subscriber)
        device.updateStatus(DeviceStatus.ON)

        assertEquals(0, subscriber.notificationCount)
    }

    @Test
    fun `multiple subscribers are notified`() {
        val device = TestDevice(name = "Device")
        val subscriber1 = MockSubscriber()
        val subscriber2 = MockSubscriber()

        device.subscribe(subscriber1)
        device.subscribe(subscriber2)
        device.updateStatus(DeviceStatus.ON)

        assertEquals(1, subscriber1.notificationCount)
        assertEquals(1, subscriber2.notificationCount)
    }

    @Test
    fun `unsubscribe stops notifications`() {
        val device = TestDevice(name = "Device")
        val subscriber = MockSubscriber()

        device.subscribe(subscriber)
        device.updateStatus(DeviceStatus.ON)
        assertEquals(1, subscriber.notificationCount)

        device.unsubscribe(subscriber)
        device.updateStatus(DeviceStatus.OFF)
        assertEquals(1, subscriber.notificationCount) // Still 1, not 2
    }

    @Test
    fun `devices with same id are equal`() {
        val id = "test-id-123"
        val device1 = TestDevice(id = id, name = "Device 1")
        val device2 = TestDevice(id = id, name = "Device 2")

        assertEquals(device1, device2)
        assertEquals(device1.hashCode(), device2.hashCode())
    }

    @Test
    fun `devices with different ids are not equal`() {
        val device1 = TestDevice(name = "Device 1")
        val device2 = TestDevice(name = "Device 2")

        assertNotEquals(device1, device2)
    }

    @Test
    fun `lighting device creation and basic operations`() {
        val light = Lighting(name = "Living Room Light")

        assertEquals("Living Room Light", light.name)
        assertEquals(DeviceStatus.OFFLINE, light.status)
        assertEquals(0, light.getBrightness())
        assertFalse(light.isLightOn())
    }

    @Test
    fun `turn on lighting device`() {
        val light = Lighting(name = "Kitchen Light")
        val subscriber = MockSubscriber()

        light.subscribe(subscriber)
        light.turnOn()

        assertTrue(light.isLightOn())
        assertEquals(DeviceStatus.ON, light.status)
        assertEquals(100, light.getBrightness())
        assertTrue(subscriber.notificationCount > 0)
    }

    @Test
    fun `turn off lighting device`() {
        val light = Lighting(name = "Bedroom Light")
        light.turnOn()

        light.turnOff()

        assertFalse(light.isLightOn())
        assertEquals(DeviceStatus.OFF, light.status)
        assertEquals(0, light.getBrightness())
    }

    @Test
    fun `set brightness on lighting device`() {
        val light = Lighting(name = "Desk Light")
        val subscriber = MockSubscriber()

        light.subscribe(subscriber)
        light.setBrightness(50)

        assertEquals(50, light.getBrightness())
        assertTrue(light.isLightOn())
        assertEquals(DeviceStatus.ON, light.status)
        assertTrue(subscriber.notificationCount > 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `set brightness above 100 throws exception`() {
        val light = Lighting(name = "Light")
        light.setBrightness(101)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `set brightness below 0 throws exception`() {
        val light = Lighting(name = "Light")
        light.setBrightness(-1)
    }

    @Test
    fun `set brightness to 0 turns off light`() {
        val light = Lighting(name = "Light")
        light.turnOn()

        light.setBrightness(0)

        assertFalse(light.isLightOn())
        assertEquals(DeviceStatus.OFF, light.status)
    }

    @Test
    fun `sensor device creation and basic operations`() {
        val sensor = Sensor(name = "Temperature Sensor", sensorType = SensorType.TEMPERATURE)

        assertEquals("Temperature Sensor", sensor.name)
        assertEquals(SensorType.TEMPERATURE, sensor.sensorType)
        assertEquals(0.0, sensor.getCurrentValue(), 0.01)
        assertEquals(DeviceStatus.OFFLINE, sensor.status)
    }

    @Test
    fun `update sensor value notifies subscribers`() {
        val sensor = Sensor(name = "Humidity Sensor", sensorType = SensorType.HUMIDITY)
        val subscriber = MockSubscriber()

        sensor.subscribe(subscriber)
        sensor.updateValue(65.5)

        assertEquals(65.5, sensor.getCurrentValue(), 0.01)
        assertEquals(1, subscriber.notificationCount)
    }

    @Test
    fun `update sensor to same value does not notify`() {
        val sensor = Sensor(name = "Motion Sensor", sensorType = SensorType.MOTION)
        val subscriber = MockSubscriber()

        sensor.updateValue(1.0)
        sensor.subscribe(subscriber)
        sensor.updateValue(1.0)

        assertEquals(0, subscriber.notificationCount)
    }

    @Test
    fun `different sensor types can be created`() {
        val tempSensor = Sensor(name = "Temp", sensorType = SensorType.TEMPERATURE)
        val motionSensor = Sensor(name = "Motion", sensorType = SensorType.MOTION)
        val smokeSensor = Sensor(name = "Smoke", sensorType = SensorType.SMOKE)

        assertEquals(SensorType.TEMPERATURE, tempSensor.sensorType)
        assertEquals(SensorType.MOTION, motionSensor.sensorType)
        assertEquals(SensorType.SMOKE, smokeSensor.sensorType)
    }
}
