package com.domotics.smarthome.viewmodel

import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceViewModelTest {

    private lateinit var viewModel: DeviceViewModel

    @Before
    fun setup() {
        viewModel = DeviceViewModel()
    }

    @Test
    fun `initial state should be empty list`() {
        assertTrue(viewModel.devices.value.isEmpty())
    }

    @Test
    fun `addDevice should add device to list`() {
        viewModel.addDevice("Living Room Light")

        val devices = viewModel.devices.value
        assertEquals(1, devices.size)
        val deviceState = devices[0]
        val device = deviceState.device as Lighting
        assertEquals("Living Room Light", device.name)
        assertEquals(DeviceStatus.OFF, deviceState.status)
        assertFalse(deviceState.isOn)
        assertEquals(0, deviceState.brightness)
    }

    @Test
    fun `addDevice with blank name should not add device`() {
        viewModel.addDevice("")
        viewModel.addDevice("   ")

        assertTrue(viewModel.devices.value.isEmpty())
    }

    @Test
    fun `addDevice should create device with OFF status`() {
        viewModel.addDevice("Kitchen Light")

        val deviceState = viewModel.devices.value[0]
        val device = deviceState.device as Lighting
        assertEquals(DeviceStatus.OFF, deviceState.status)
        assertFalse(device.isLightOn())
    }

    @Test
    fun `removeDevice should remove device from list`() {
        viewModel.addDevice("Device 1")
        val devicesAfterAdd = viewModel.devices.value
        assertEquals(1, devicesAfterAdd.size)

        viewModel.removeDevice(devicesAfterAdd[0])
        assertTrue(viewModel.devices.value.isEmpty())
    }

    @Test
    fun `removeDevice should only remove specific device`() {
        viewModel.addDevice("Device 1")
        viewModel.addDevice("Device 2")
        val twoDevices = viewModel.devices.value
        assertEquals(2, twoDevices.size)

        viewModel.removeDevice(twoDevices[0])
        val oneDevice = viewModel.devices.value
        assertEquals(1, oneDevice.size)
        assertEquals("Device 2", (oneDevice[0].device as Lighting).name)
    }

    @Test
    fun `toggleDevice should turn on lighting device`() {
        viewModel.addDevice("Test Light")
        val deviceState = viewModel.devices.value[0]
        val device = deviceState.device as Lighting

        assertFalse(device.isLightOn())
        assertEquals(DeviceStatus.OFF, deviceState.status)

        viewModel.toggleDevice(deviceState)
        val updatedState = viewModel.devices.value[0]
        val updatedDevice = updatedState.device as Lighting

        assertTrue(updatedDevice.isLightOn())
        assertEquals(DeviceStatus.ON, updatedState.status)
        assertEquals(100, updatedState.brightness)
    }

    @Test
    fun `toggleDevice should turn off lighting device`() {
        viewModel.addDevice("Test Light")
        val deviceState = viewModel.devices.value[0]

        viewModel.toggleDevice(deviceState)
        val onState = viewModel.devices.value[0]
        assertTrue((onState.device as Lighting).isLightOn())

        viewModel.toggleDevice(onState)
        val offState = viewModel.devices.value[0]
        val offDevice = offState.device as Lighting

        assertFalse(offDevice.isLightOn())
        assertEquals(DeviceStatus.OFF, offState.status)
        assertEquals(0, offState.brightness)
    }

    @Test
    fun `multiple devices can be added and managed independently`() {
        viewModel.addDevice("Light 1")
        viewModel.addDevice("Light 2")
        viewModel.addDevice("Light 3")

        val devices = viewModel.devices.value
        assertEquals(3, devices.size)
        assertEquals("Light 1", (devices[0].device as Lighting).name)
        assertEquals("Light 2", (devices[1].device as Lighting).name)
        assertEquals("Light 3", (devices[2].device as Lighting).name)

        viewModel.toggleDevice(devices[0])
        val updated = viewModel.devices.value
        val light1 = updated[0].device as Lighting
        val light2 = updated[1].device as Lighting

        assertTrue(light1.isLightOn())
        assertFalse(light2.isLightOn())
    }

    @Test
    fun `devices should have unique IDs`() {
        viewModel.addDevice("Device 1")
        viewModel.addDevice("Device 2")
        viewModel.addDevice("Device 3")

        val devices = viewModel.devices.value
        val ids = devices.map { it.device.id }.toSet()

        assertEquals(3, ids.size)
    }
}
