from .repositories import BuildingRepository, LocationRepository, RoomRepository
from .services.building_service import BuildingService
from .services.location_service import LocationService
from .services.room_service import RoomService

location_repository = LocationRepository()
building_repository = BuildingRepository()
room_repository = RoomRepository()

location_service = LocationService(location_repository)
building_service = BuildingService(location_repository, building_repository)
room_service = RoomService(building_repository, room_repository)


def reset_repositories() -> None:
    location_repository.clear()
    building_repository.clear()
    room_repository.clear()
