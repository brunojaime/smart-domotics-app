package com.domotics.smarthome.viewmodel

import app.cash.turbine.test
import com.domotics.smarthome.entities.DeviceStatus
import com.domotics.smarthome.entities.Lighting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceViewModelTest {

    private lateinit var viewModel: DeviceViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DeviceViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty list`() = runTest {
        viewModel.devices.test {
            val initialState = awaitItem()
            assertTrue(initialState.isEmpty())
        }
    }

    @Test
    fun `addDevice should add device to list`() = runTest {
        viewModel.devices.test {
            // Skip initial empty state
            awaitItem()

            viewModel.addDevice("Living Room Light")

            val devices = awaitItem()
            assertEquals(1, devices.size)
            assertEquals("Living Room Light", devices[0].name)
            assertTrue(devices[0] is Lighting)
        }
    }

    @Test
    fun `addDevice with blank name should not add device`() = runTest {
        viewModel.devices.test {
            awaitItem() // initial state

            viewModel.addDevice("")
            viewModel.addDevice("   ")

            // No new emissions should occur
            expectNoEvents()
        }
    }

    @Test
    fun `addDevice should create device with OFF status`() = runTest {
        viewModel.addDevice("Kitchen Light")

        viewModel.devices.test {
            val devices = awaitItem()
            assertEquals(DeviceStatus.OFF, devices[0].status)
            assertTrue(devices[0] is Lighting)
            assertFalse((devices[0] as Lighting).isLightOn())
        }
    }

    @Test
    fun `removeDevice should remove device from list`() = runTest {
        viewModel.devices.test {
            awaitItem() // initial

            viewModel.addDevice("Device 1")
            val devicesAfterAdd = awaitItem()
            assertEquals(1, devicesAfterAdd.size)

            viewModel.removeDevice(devicesAfterAdd[0])
            val devicesAfterRemove = awaitItem()
            assertTrue(devicesAfterRemove.isEmpty())
        }
    }

    @Test
    fun `removeDevice should only remove specific device`() = runTest {
        viewModel.devices.test {
            awaitItem() // initial

            viewModel.addDevice("Device 1")
            awaitItem()

            viewModel.addDevice("Device 2")
            val twoDevices = awaitItem()
            assertEquals(2, twoDevices.size)

            viewModel.removeDevice(twoDevices[0])
            val oneDevice = awaitItem()
            assertEquals(1, oneDevice.size)
            assertEquals("Device 2", oneDevice[0].name)
        }
    }

    @Test
    fun `toggleDevice should turn on lighting device`() = runTest {
        viewModel.devices.test {
            awaitItem() // initial

            viewModel.addDevice("Test Light")
            val devices = awaitItem()
            val device = devices[0] as Lighting

            assertFalse(device.isLightOn())
            assertEquals(DeviceStatus.OFF, device.status)

            viewModel.toggleDevice(device)
            val updatedDevices = awaitItem()
            val updatedDevice = updatedDevices[0] as Lighting

            assertTrue(updatedDevice.isLightOn())
            assertEquals(DeviceStatus.ON, updatedDevice.status)
            assertEquals(100, updatedDevice.getBrightness())
        }
    }

    @Test
    fun `toggleDevice should turn off lighting device`() = runTest {
        viewModel.devices.test {
            awaitItem() // initial

            viewModel.addDevice("Test Light")
            val devices = awaitItem()
            val device = devices[0] as Lighting

            // Turn on first
            viewModel.toggleDevice(device)
            val onDevices = awaitItem()
            val onDevice = onDevices[0] as Lighting
            assertTrue(onDevice.isLightOn())

            // Turn off
            viewModel.toggleDevice(onDevice)
            val offDevices = awaitItem()
            val offDevice = offDevices[0] as Lighting

            assertFalse(offDevice.isLightOn())
            assertEquals(DeviceStatus.OFF, offDevice.status)
            assertEquals(0, offDevice.getBrightness())
        }
    }

    @Test
    fun `multiple devices can be added and managed independently`() = runTest {
        viewModel.devices.test {
            awaitItem() // initial

            viewModel.addDevice("Light 1")
            awaitItem()

            viewModel.addDevice("Light 2")
            awaitItem()

            viewModel.addDevice("Light 3")
            val devices = awaitItem()

            assertEquals(3, devices.size)
            assertEquals("Light 1", devices[0].name)
            assertEquals("Light 2", devices[1].name)
            assertEquals("Light 3", devices[2].name)

            // Toggle first device
            viewModel.toggleDevice(devices[0])
            val updated = awaitItem()
            val light1 = updated[0] as Lighting
            val light2 = updated[1] as Lighting

            assertTrue(light1.isLightOn())
            assertFalse(light2.isLightOn())
        }
    }

    @Test
    fun `devices should have unique IDs`() = runTest {
        viewModel.addDevice("Device 1")
        viewModel.addDevice("Device 2")
        viewModel.addDevice("Device 3")

        viewModel.devices.test {
            val devices = awaitItem()
            val ids = devices.map { it.id }.toSet()

            assertEquals(3, ids.size) // All IDs are unique
        }
    }
}
