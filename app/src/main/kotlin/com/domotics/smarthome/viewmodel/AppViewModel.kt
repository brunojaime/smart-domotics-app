package com.domotics.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domotics.smarthome.data.remote.ApiResult
import com.domotics.smarthome.data.remote.CreateBuildingRequest
import com.domotics.smarthome.data.remote.CreateLocationRequest
import com.domotics.smarthome.data.remote.CreateZoneRequest
import com.domotics.smarthome.data.remote.DomoticsApiClient
import com.domotics.smarthome.data.remote.SmartHomeRepository
import com.domotics.smarthome.entities.Building
import com.domotics.smarthome.entities.Location
import com.domotics.smarthome.entities.Zone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository(
        DomoticsApiClient.create().smartHomeApiService,
    ),
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _username = MutableStateFlow("Guest")
    val username: StateFlow<String> = _username

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _crudError = MutableStateFlow<String?>(null)
    val crudError: StateFlow<String?> = _crudError

    private val _selectedDestination = MutableStateFlow(AppDestination.Devices)
    val selectedDestination: StateFlow<AppDestination> = _selectedDestination

    private val _buildings = MutableStateFlow<List<CrudItem>>(emptyList())
    val buildings: StateFlow<List<CrudItem>> = _buildings

    private val _locations = MutableStateFlow<List<CrudItem>>(emptyList())
    val locations: StateFlow<List<CrudItem>> = _locations

    private val _zones = MutableStateFlow<List<CrudItem>>(emptyList())
    val zones: StateFlow<List<CrudItem>> = _zones

    private val _sensors = MutableStateFlow(sampleRecords("Temperature sensor", "CO2 detector"))
    val sensors: StateFlow<List<CrudItem>> = _sensors

    private val _users = MutableStateFlow(sampleRecords("Admin user", "Guest user"))
    val users: StateFlow<List<CrudItem>> = _users

    private val _accessControls = MutableStateFlow(sampleRecords("Front door", "Garage"))
    val accessControls: StateFlow<List<CrudItem>> = _accessControls

    private val _selectedLocationId = MutableStateFlow<String?>(null)
    val selectedLocationId: StateFlow<String?> = _selectedLocationId

    private val _selectedBuildingId = MutableStateFlow<String?>(null)
    val selectedBuildingId: StateFlow<String?> = _selectedBuildingId

    init {
        refreshLocations()
    }

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
            ManagedSection.BUILDING -> saveBuilding(id, name)
            ManagedSection.LOCATION -> saveLocation(id, name)
            ManagedSection.ZONE -> saveZone(id, name)
            ManagedSection.SENSOR -> saveRecord(_sensors, id, name, description)
            ManagedSection.USER -> saveRecord(_users, id, name, description)
            ManagedSection.ACCESS -> saveRecord(_accessControls, id, name, description)
        }
    }

    fun deleteRecord(section: ManagedSection, id: String) {
        when (section) {
            ManagedSection.BUILDING -> deleteBuilding(id)
            ManagedSection.LOCATION -> deleteLocation(id)
            ManagedSection.ZONE -> deleteZone(id)
            ManagedSection.SENSOR -> deleteRecord(_sensors, id)
            ManagedSection.USER -> deleteRecord(_users, id)
            ManagedSection.ACCESS -> deleteRecord(_accessControls, id)
        }
    }

    fun selectLocation(locationId: String) {
        if (locationId == _selectedLocationId.value) return
        _crudError.value = null
        _selectedLocationId.value = locationId
        _selectedBuildingId.value = null
        refreshBuildings(locationId)
    }

    fun selectBuilding(buildingId: String) {
        if (buildingId == _selectedBuildingId.value) return
        _crudError.value = null
        _selectedBuildingId.value = buildingId
        val locationId = _selectedLocationId.value ?: return
        refreshZones(locationId, buildingId)
    }

    fun clearCrudError() {
        _crudError.value = null
    }

    private fun refreshLocations() {
        viewModelScope.launch {
            repository.fetchLocations().collect { result ->
                when (result) {
                    is ApiResult.Success -> setLocations(result.data)
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun refreshBuildings(locationId: String? = _selectedLocationId.value) {
        if (locationId.isNullOrBlank()) {
            _buildings.value = emptyList()
            _zones.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.fetchBuildings(locationId).collect { result ->
                when (result) {
                    is ApiResult.Success -> setBuildings(locationId, result.data)
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun refreshZones(
        locationId: String? = _selectedLocationId.value,
        buildingId: String? = _selectedBuildingId.value,
    ) {
        if (locationId.isNullOrBlank() || buildingId.isNullOrBlank()) {
            _zones.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.fetchZones(locationId, buildingId).collect { result ->
                when (result) {
                    is ApiResult.Success -> _zones.value = result.data.map { it.toCrudItem() }
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun saveLocation(id: String?, name: String) {
        _crudError.value = null
        viewModelScope.launch {
            val request = CreateLocationRequest(name = name)
            val flow = if (id == null) {
                repository.createLocation(request)
            } else {
                repository.updateLocation(id, request)
            }
            flow.collect { result ->
                when (result) {
                    is ApiResult.Success -> refreshLocations()
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun deleteLocation(id: String) {
        _crudError.value = null
        viewModelScope.launch {
            repository.deleteLocation(id).collect { result ->
                when (result) {
                    is ApiResult.Success -> refreshLocations()
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun saveBuilding(id: String?, name: String) {
        val locationId = _selectedLocationId.value
        if (locationId.isNullOrBlank()) {
            _crudError.value = "Select a location first."
            return
        }
        _crudError.value = null
        viewModelScope.launch {
            val request = CreateBuildingRequest(name = name)
            val flow = if (id == null) {
                repository.createBuilding(locationId, request)
            } else {
                repository.updateBuilding(locationId, id, request)
            }
            flow.collect { result ->
                when (result) {
                    is ApiResult.Success -> refreshBuildings(locationId)
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun deleteBuilding(id: String) {
        val locationId = _selectedLocationId.value
        if (locationId.isNullOrBlank()) {
            _crudError.value = "Select a location first."
            return
        }
        _crudError.value = null
        viewModelScope.launch {
            repository.deleteBuilding(locationId, id).collect { result ->
                when (result) {
                    is ApiResult.Success -> refreshBuildings(locationId)
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun saveZone(id: String?, name: String) {
        val locationId = _selectedLocationId.value
        val buildingId = _selectedBuildingId.value
        if (locationId.isNullOrBlank() || buildingId.isNullOrBlank()) {
            _crudError.value = "Select a location and building first."
            return
        }
        _crudError.value = null
        viewModelScope.launch {
            val request = CreateZoneRequest(name = name)
            val flow = if (id == null) {
                repository.createZone(locationId, buildingId, request)
            } else {
                repository.updateZone(locationId, buildingId, id, request)
            }
            flow.collect { result ->
                when (result) {
                    is ApiResult.Success -> refreshZones(locationId, buildingId)
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    private fun deleteZone(id: String) {
        val locationId = _selectedLocationId.value
        val buildingId = _selectedBuildingId.value
        if (locationId.isNullOrBlank() || buildingId.isNullOrBlank()) {
            _crudError.value = "Select a location and building first."
            return
        }
        _crudError.value = null
        viewModelScope.launch {
            repository.deleteZone(locationId, buildingId, id).collect { result ->
                when (result) {
                    is ApiResult.Success -> refreshZones(locationId, buildingId)
                    is ApiResult.Error -> _crudError.value = result.message
                    ApiResult.Loading -> Unit
                }
            }
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

    private fun setLocations(locations: List<Location>) {
        _locations.value = locations.map { it.toCrudItem() }
        val selectedId = _selectedLocationId.value
        val nextId = locations.firstOrNull { it.id == selectedId }?.id ?: locations.firstOrNull()?.id
        if (nextId != null && nextId != selectedId) {
            _selectedLocationId.value = nextId
        }
        refreshBuildings(nextId)
    }

    private fun setBuildings(locationId: String, buildings: List<Building>) {
        _buildings.value = buildings.map { it.toCrudItem() }
        val selectedId = _selectedBuildingId.value
        val nextId = buildings.firstOrNull { it.id == selectedId }?.id ?: buildings.firstOrNull()?.id
        if (nextId != null && nextId != selectedId) {
            _selectedBuildingId.value = nextId
        }
        refreshZones(locationId, nextId)
    }
}

data class CrudItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
)

private fun Location.toCrudItem(): CrudItem =
    CrudItem(
        id = id,
        name = name,
        description = "Buildings: ${buildingIds.size}",
    )

private fun Building.toCrudItem(): CrudItem =
    CrudItem(
        id = id,
        name = name,
        description = "Zones: ${zoneIds.size}",
    )

private fun Zone.toCrudItem(): CrudItem =
    CrudItem(
        id = id,
        name = name,
        description = "Areas: ${areaIds.size}",
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
