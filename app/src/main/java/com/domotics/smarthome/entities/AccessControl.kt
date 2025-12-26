package com.domotics.smarthome.entities

/**
 * Simple registry to manage available roles and permissions.
 */
class AccessControl {
    private val rolesByName: MutableMap<String, Role> = mutableMapOf()

    init {
        registerDefaultRoles()
    }

    /**
     * Create and register a new role with the provided permissions.
     * Throws an exception if the role name already exists.
     */
    fun createRole(name: String, permissions: Set<Permission>, description: String? = null): Role {
        require(!rolesByName.containsKey(name.lowercase())) { "Role with name $name already exists" }
        val role = Role(name = name, permissions = permissions.toMutableSet(), description = description)
        rolesByName[name.lowercase()] = role
        return role
    }

    /**
     * Retrieve an existing role by its name.
     */
    fun getRole(name: String): Role? = rolesByName[name.lowercase()]

    private fun registerDefaultRoles() {
        val allPermissions = Permission.values().toMutableSet()
        val adminRole = Role(name = "Administrator", permissions = allPermissions, description = "Full access to manage the system")
        val userRole = Role(
            name = "User",
            permissions = mutableSetOf(Permission.VIEW_DEVICES, Permission.CONTROL_DEVICES),
            description = "Standard user who can view and control assigned devices"
        )

        rolesByName[adminRole.name.lowercase()] = adminRole
        rolesByName[userRole.name.lowercase()] = userRole
    }
}
