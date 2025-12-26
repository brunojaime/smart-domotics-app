from .repositories import get_repository_provider
from .services.area_service import AreaService
from .services.building_service import BuildingService
from .services.location_service import LocationService
from .services.zone_device_service import ZoneDeviceService
from .services.zone_service import ZoneService

repository_provider = get_repository_provider()

location_service = LocationService(repository_provider.locations)
building_service = BuildingService(repository_provider.locations, repository_provider.buildings)
zone_service = ZoneService(
    repository_provider.locations,
    repository_provider.buildings,
    repository_provider.zones,
)
area_service = AreaService(
    repository_provider.locations,
    repository_provider.buildings,
    repository_provider.zones,
    repository_provider.areas,
)
zone_device_service = ZoneDeviceService(
    repository_provider.zones,
    repository_provider.zone_devices,
)


def reset_repositories() -> None:
    global repository_provider, location_service, building_service, zone_service, area_service, zone_device_service
    repository_provider = get_repository_provider()
    location_service = LocationService(repository_provider.locations)
    building_service = BuildingService(repository_provider.locations, repository_provider.buildings)
    zone_service = ZoneService(
        repository_provider.locations,
        repository_provider.buildings,
        repository_provider.zones,
    )
    area_service = AreaService(
        repository_provider.locations,
        repository_provider.buildings,
        repository_provider.zones,
        repository_provider.areas,
    )
    zone_device_service = ZoneDeviceService(
        repository_provider.zones,
        repository_provider.zone_devices,
    )
