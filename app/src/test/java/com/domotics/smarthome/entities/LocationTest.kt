package com.domotics.smarthome.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocationTest {

    @Test
    fun `create location with default id`() {
        val location = Location(name = "Campus")

        assertEquals("Campus", location.name)
        assertNotEquals("", location.id)
        assertEquals(emptyList<String>(), location.buildingIds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name throws exception`() {
        Location(name = " ")
    }

    @Test
    fun `building ids are preserved`() {
        val location = Location(name = "HQ", buildingIds = listOf("b1", "b2"))

        assertEquals(listOf("b1", "b2"), location.buildingIds)
    }
}
