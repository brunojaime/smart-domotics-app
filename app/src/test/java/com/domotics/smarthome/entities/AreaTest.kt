package com.domotics.smarthome.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AreaTest {

    @Test
    fun `create area with all properties`() {
        val area = Area(
            name = "Living Room",
            zoneId = "zone-1",
        )

        assertNotEquals("", area.id)
        assertEquals("Living Room", area.name)
        assertEquals("zone-1", area.zoneId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Area(name = "", zoneId = "zone-1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank zone id throws exception`() {
        Area(name = "Kitchen", zoneId = " ")
    }

    @Test
    fun `custom id is preserved`() {
        val customId = "custom-area-id-123"
        val area = Area(id = customId, name = "Custom Area", zoneId = "zone-1")

        assertEquals(customId, area.id)
    }
}
