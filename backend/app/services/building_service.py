from __future__ import annotations

from uuid import uuid4

from ..models import Building, BuildingCreate, BuildingUpdate
from ..repositories import BuildingRepository, LocationRepository


class BuildingService:
    def __init__(self, location_repository: LocationRepository, building_repository: BuildingRepository) -> None:
        self._location_repository = location_repository
        self._building_repository = building_repository

    def list_buildings(self, location_id: str) -> list[Building]:
        self._ensure_location_exists(location_id)
        return self._building_repository.list_by_location(location_id)

    def create_building(self, location_id: str, data: BuildingCreate) -> Building:
        self._ensure_location_exists(location_id)
        building = Building(id=str(uuid4()), name=data.name, location_id=location_id)
        return self._building_repository.add(building)

    def get_building(self, location_id: str, building_id: str) -> Building:
        self._ensure_location_exists(location_id)
        building = self._building_repository.get(location_id, building_id)
        if building is None:
            raise KeyError("Building not found")
        return building

    def update_building(self, location_id: str, building_id: str, data: BuildingUpdate) -> Building:
        building = self.get_building(location_id, building_id)
        if data.name is None:
            raise ValueError("No updates provided")
        updated = building.model_copy(update={"name": data.name})
        return self._building_repository.update(updated)

    def delete_building(self, location_id: str, building_id: str) -> None:
        self._ensure_location_exists(location_id)
        self._building_repository.delete(location_id, building_id)

    def _ensure_location_exists(self, location_id: str) -> None:
        if self._location_repository.get(location_id) is None:
            raise KeyError("Location not found")
