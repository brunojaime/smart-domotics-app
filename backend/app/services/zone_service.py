from __future__ import annotations

from uuid import uuid4

from ..models import Zone, ZoneCreate, ZoneUpdate
from ..repositories import BuildingRepository, ZoneRepository


class ZoneService:
    def __init__(self, building_repository: BuildingRepository, zone_repository: ZoneRepository) -> None:
        self._building_repository = building_repository
        self._zone_repository = zone_repository

    def list_zones(self, location_id: str, building_id: str) -> list[Zone]:
        self._ensure_building_exists(location_id, building_id)
        return self._zone_repository.list_by_building(building_id)

    def create_zone(self, location_id: str, building_id: str, data: ZoneCreate) -> Zone:
        self._ensure_building_exists(location_id, building_id)
        zone = Zone(id=str(uuid4()), name=data.name, building_id=building_id)
        return self._zone_repository.add(zone)

    def get_zone(self, location_id: str, building_id: str, zone_id: str) -> Zone:
        self._ensure_building_exists(location_id, building_id)
        zone = self._zone_repository.get(building_id, zone_id)
        if zone is None:
            raise KeyError("Zone not found")
        return zone

    def update_zone(self, location_id: str, building_id: str, zone_id: str, data: ZoneUpdate) -> Zone:
        zone = self.get_zone(location_id, building_id, zone_id)
        if data.name is None:
            raise ValueError("No updates provided")
        updated = zone.model_copy(update={"name": data.name})
        return self._zone_repository.update(updated)

    def delete_zone(self, location_id: str, building_id: str, zone_id: str) -> None:
        self._ensure_building_exists(location_id, building_id)
        self._zone_repository.delete(building_id, zone_id)

    def delete_zones_for_building(self, building_id: str) -> None:
        self._zone_repository.delete_by_building(building_id)

    def _ensure_building_exists(self, location_id: str, building_id: str) -> None:
        if self._building_repository.get(location_id, building_id) is None:
            raise KeyError("Building not found")
