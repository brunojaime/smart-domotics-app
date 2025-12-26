from __future__ import annotations

from ..domain.entities import Zone
from ..dto.structures import ZoneCreateRequest, ZoneResponse, ZoneUpdateRequest
from ..repositories.base import BuildingRepository, LocationRepository, ZoneRepository


class ZoneService:
    def __init__(
        self,
        location_repository: LocationRepository,
        building_repository: BuildingRepository,
        zone_repository: ZoneRepository,
    ) -> None:
        self._location_repository = location_repository
        self._building_repository = building_repository
        self._zone_repository = zone_repository

    def list_zones(self, location_id: str, building_id: str) -> list[ZoneResponse]:
        self._ensure_building_exists(location_id, building_id)
        return [
            self._to_response(zone)
            for zone in self._zone_repository.list_for_building(building_id)
        ]

    def create_zone(self, location_id: str, building_id: str, data: ZoneCreateRequest) -> ZoneResponse:
        self._ensure_building_exists(location_id, building_id)
        zone = self._zone_repository.create(data.name, building_id)
        return self._to_response(zone)

    def get_zone(self, location_id: str, building_id: str, zone_id: str) -> ZoneResponse:
        self._ensure_building_exists(location_id, building_id)
        zone = self._get_zone(zone_id)
        if zone.building_id != building_id:
            raise KeyError("Zone not found")
        return self._to_response(zone)

    def update_zone(
        self,
        location_id: str,
        building_id: str,
        zone_id: str,
        data: ZoneUpdateRequest,
    ) -> ZoneResponse:
        self._ensure_building_exists(location_id, building_id)
        zone = self._get_zone(zone_id)
        if zone.building_id != building_id:
            raise KeyError("Zone not found")
        if data.name is None:
            raise ValueError("No updates provided")
        updated = Zone(id=zone.id, name=data.name, building_id=zone.building_id, areas=zone.areas)
        return self._to_response(self._zone_repository.update(updated))

    def delete_zone(self, location_id: str, building_id: str, zone_id: str) -> None:
        self._ensure_building_exists(location_id, building_id)
        zone = self._get_zone(zone_id)
        if zone.building_id != building_id:
            raise KeyError("Zone not found")
        self._zone_repository.delete(zone_id)

    def _ensure_building_exists(self, location_id: str, building_id: str) -> None:
        if self._location_repository.get(location_id) is None:
            raise KeyError("Location not found")
        building = self._building_repository.get(building_id)
        if building is None or building.location_id != location_id:
            raise KeyError("Building not found")

    def _get_zone(self, zone_id: str) -> Zone:
        zone = self._zone_repository.get(zone_id)
        if zone is None:
            raise KeyError("Zone not found")
        return zone

    @staticmethod
    def _to_response(zone: Zone) -> ZoneResponse:
        return ZoneResponse(
            id=zone.id,
            name=zone.name,
            building_id=zone.building_id,
            area_ids=[area.id for area in zone.areas],
        )
