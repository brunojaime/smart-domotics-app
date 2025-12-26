from __future__ import annotations

from ..domain.entities import Building
from ..dto.structures import BuildingCreateRequest, BuildingResponse, BuildingUpdateRequest
from ..repositories.base import BuildingRepository, LocationRepository


class BuildingService:
    def __init__(self, location_repository: LocationRepository, building_repository: BuildingRepository) -> None:
        self._location_repository = location_repository
        self._building_repository = building_repository

    def list_buildings(self, location_id: str) -> list[BuildingResponse]:
        self._ensure_location_exists(location_id)
        return [
            self._to_response(building)
            for building in self._building_repository.list_for_location(location_id)
        ]

    def create_building(self, location_id: str, data: BuildingCreateRequest) -> BuildingResponse:
        self._ensure_location_exists(location_id)
        building = self._building_repository.create(data.name, location_id)
        return self._to_response(building)

    def get_building(self, location_id: str, building_id: str) -> BuildingResponse:
        self._ensure_location_exists(location_id)
        building = self._get_building(building_id)
        if building.location_id != location_id:
            raise KeyError("Building not found")
        return self._to_response(building)

    def update_building(self, location_id: str, building_id: str, data: BuildingUpdateRequest) -> BuildingResponse:
        building = self._get_building(building_id)
        if building.location_id != location_id:
            raise KeyError("Building not found")
        if data.name is None:
            raise ValueError("No updates provided")
        updated = Building(
            id=building.id,
            name=data.name,
            location_id=building.location_id,
            zones=building.zones,
        )
        return self._to_response(self._building_repository.update(updated))

    def delete_building(self, location_id: str, building_id: str) -> None:
        self._ensure_location_exists(location_id)
        building = self._get_building(building_id)
        if building.location_id != location_id:
            raise KeyError("Building not found")
        self._building_repository.delete(building_id)

    def _ensure_location_exists(self, location_id: str) -> None:
        if self._location_repository.get(location_id) is None:
            raise KeyError("Location not found")

    def _get_building(self, building_id: str) -> Building:
        building = self._building_repository.get(building_id)
        if building is None:
            raise KeyError("Building not found")
        return building

    @staticmethod
    def _to_response(building: Building) -> BuildingResponse:
        return BuildingResponse(
            id=building.id,
            name=building.name,
            location_id=building.location_id,
            zone_ids=[zone.id for zone in building.zones],
        )
