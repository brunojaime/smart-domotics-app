package com.domotics.smarthome.entities

import java.util.UUID

/**
 * Represents a role that aggregates permissions.
 */
data class Role(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val permissions: MutableSet<Permission> = mutableSetOf(),
    val description: String? = null
) {
    init {
        require(name.isNotBlank()) { "Role name cannot be blank" }
    }

    /**
     * Add a permission to this role.
     */
    fun addPermission(permission: Permission) {
        permissions.add(permission)
    }

    /**
     * Remove a permission from this role.
     */
    fun removePermission(permission: Permission) {
        permissions.remove(permission)
    }

    /**
     * Check if the role grants a specific permission.
     */
    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)
}
