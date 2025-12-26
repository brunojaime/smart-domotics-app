package com.domotics.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AppViewModel : ViewModel() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _username = MutableStateFlow("Guest")
    val username: StateFlow<String> = _username

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _selectedDestination = MutableStateFlow(AppDestination.Devices)
    val selectedDestination: StateFlow<AppDestination> = _selectedDestination

    private val _buildings = MutableStateFlow(sampleRecords("Headquarters", "Warehouse"))
    val buildings: StateFlow<List<CrudItem>> = _buildings

    private val _locations = MutableStateFlow(sampleRecords("Lobby", "Server room"))
    val locations: StateFlow<List<CrudItem>> = _locations

    private val _zones = MutableStateFlow(sampleRecords("Security perimeter", "Garden"))
    val zones: StateFlow<List<CrudItem>> = _zones

    private val _sensors = MutableStateFlow(sampleRecords("Temperature sensor", "CO2 detector"))
    val sensors: StateFlow<List<CrudItem>> = _sensors

    private val _users = MutableStateFlow(sampleRecords("Admin user", "Guest user"))
    val users: StateFlow<List<CrudItem>> = _users

    private val _accessControls = MutableStateFlow(sampleRecords("Front door", "Garage"))
    val accessControls: StateFlow<List<CrudItem>> = _accessControls

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authError.value = "Please enter both a username and password."
            _isAuthenticated.value = false
            return
        }
        _authError.value = null
        _username.value = username
        _isAuthenticated.value = true
    }

    fun logout() {
        _isAuthenticated.value = false
        _selectedDestination.value = AppDestination.Auth
    }

    fun selectDestination(destination: AppDestination) {
        _selectedDestination.value = destination
    }

    fun saveRecord(section: ManagedSection, id: String?, name: String, description: String) {
        when (section) {
            ManagedSection.BUILDING -> saveRecord(_buildings, id, name, description)
            ManagedSection.LOCATION -> saveRecord(_locations, id, name, description)
            ManagedSection.ZONE -> saveRecord(_zones, id, name, description)
            ManagedSection.SENSOR -> saveRecord(_sensors, id, name, description)
            ManagedSection.USER -> saveRecord(_users, id, name, description)
            ManagedSection.ACCESS -> saveRecord(_accessControls, id, name, description)
        }
    }

    fun deleteRecord(section: ManagedSection, id: String) {
        when (section) {
            ManagedSection.BUILDING -> deleteRecord(_buildings, id)
            ManagedSection.LOCATION -> deleteRecord(_locations, id)
            ManagedSection.ZONE -> deleteRecord(_zones, id)
            ManagedSection.SENSOR -> deleteRecord(_sensors, id)
            ManagedSection.USER -> deleteRecord(_users, id)
            ManagedSection.ACCESS -> deleteRecord(_accessControls, id)
        }
    }

    private fun saveRecord(
        state: MutableStateFlow<List<CrudItem>>,
        id: String?,
        name: String,
        description: String,
    ) {
        val current = state.value
        if (id == null) {
            state.value = current + CrudItem(name = name, description = description)
            return
        }

        state.value = current.map {
            if (it.id == id) it.copy(name = name, description = description) else it
        }
    }

    private fun deleteRecord(state: MutableStateFlow<List<CrudItem>>, id: String) {
        state.value = state.value.filterNot { it.id == id }
    }

    private fun sampleRecords(vararg names: String): List<CrudItem> {
        return names.map {
            CrudItem(name = it, description = "Tap edit to update ${it.lowercase()} settings")
        }
    }
}

data class CrudItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
)

enum class ManagedSection(val label: String) {
    BUILDING("Buildings"),
    LOCATION("Locations"),
    ZONE("Zones"),
    SENSOR("Sensors"),
    USER("Users"),
    ACCESS("Access control"),
}

enum class AppDestination(val label: String, val requiresAuth: Boolean) {
    Devices("Devices", true),
    Buildings("Buildings", true),
    Locations("Locations", true),
    Zones("Zones", true),
    Sensors("Sensors", true),
    Users("Users", true),
    Access("Access control", true),
    Profile("Profile", true),
    Auth("Authentication", false),
}
