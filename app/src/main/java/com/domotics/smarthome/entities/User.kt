package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a user that can interact with the domotics system.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val factories: MutableSet<Building> = mutableSetOf(),
    val roles: MutableSet<Role> = mutableSetOf()
) {
    init {
        require(name.isNotBlank()) { "User name cannot be blank" }
        require(email.contains("@")) { "User email must be valid" }
    }

    /**
     * Associate the user with a new factory or building.
     */
    fun assignFactory(building: Building) {
        factories.add(building)
    }

    /**
     * Remove the association between the user and a factory or building.
     */
    fun revokeFactory(building: Building) {
        factories.remove(building)
    }

    /**
     * Attach a role to the user.
     */
    fun assignRole(role: Role) {
        roles.add(role)
    }

    /**
     * Remove a role from the user.
     */
    fun revokeRole(role: Role) {
        roles.remove(role)
    }

    /**
     * Check if the user has a given permission through any of their roles.
     */
    fun hasPermission(permission: Permission): Boolean =
        roles.any { it.hasPermission(permission) }
}
