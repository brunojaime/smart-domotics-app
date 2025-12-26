from .repositories import BuildingRepository, LocationRepository, RoomRepository, ZoneDeviceRepository, ZoneRepository
from .services.building_service import BuildingService
from .services.location_service import LocationService
from .services.room_service import RoomService
from .services.zone_device_service import ZoneDeviceService
from .services.zone_service import ZoneService

location_repository = LocationRepository()
building_repository = BuildingRepository()
room_repository = RoomRepository()
zone_repository = ZoneRepository()
zone_device_repository = ZoneDeviceRepository()

location_service = LocationService(location_repository)
building_service = BuildingService(location_repository, building_repository)
room_service = RoomService(building_repository, room_repository)
zone_service = ZoneService(building_repository, zone_repository)
zone_device_service = ZoneDeviceService(zone_repository, zone_device_repository)


def reset_repositories() -> None:
    location_repository.clear()
    building_repository.clear()
    room_repository.clear()
    zone_repository.clear()
    zone_device_repository.clear()
