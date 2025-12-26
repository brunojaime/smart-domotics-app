from __future__ import annotations

from ..domain.entities import Area
from ..dto.structures import AreaCreateRequest, AreaResponse, AreaUpdateRequest
from ..repositories.base import AreaRepository, BuildingRepository, LocationRepository, ZoneRepository


class AreaService:
    def __init__(
        self,
        location_repository: LocationRepository,
        building_repository: BuildingRepository,
        zone_repository: ZoneRepository,
        area_repository: AreaRepository,
    ) -> None:
        self._location_repository = location_repository
        self._building_repository = building_repository
        self._zone_repository = zone_repository
        self._area_repository = area_repository

    def list_areas(
        self,
        location_id: str,
        building_id: str,
        zone_id: str,
    ) -> list[AreaResponse]:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        return [self._to_response(area) for area in self._area_repository.list_for_zone(zone_id)]

    def create_area(
        self,
        location_id: str,
        building_id: str,
        zone_id: str,
        data: AreaCreateRequest,
    ) -> AreaResponse:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        area = self._area_repository.create(data.name, zone_id)
        return self._to_response(area)

    def get_area(
        self,
        location_id: str,
        building_id: str,
        zone_id: str,
        area_id: str,
    ) -> AreaResponse:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        area = self._get_area(area_id)
        if area.zone_id != zone_id:
            raise KeyError("Area not found")
        return self._to_response(area)

    def update_area(
        self,
        location_id: str,
        building_id: str,
        zone_id: str,
        area_id: str,
        data: AreaUpdateRequest,
    ) -> AreaResponse:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        area = self._get_area(area_id)
        if area.zone_id != zone_id:
            raise KeyError("Area not found")
        if data.name is None:
            raise ValueError("No updates provided")
        updated = Area(id=area.id, name=data.name, zone_id=area.zone_id)
        return self._to_response(self._area_repository.update(updated))

    def delete_area(
        self,
        location_id: str,
        building_id: str,
        zone_id: str,
        area_id: str,
    ) -> None:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        area = self._get_area(area_id)
        if area.zone_id != zone_id:
            raise KeyError("Area not found")
        self._area_repository.delete(area_id)

    def _ensure_zone_exists(self, location_id: str, building_id: str, zone_id: str) -> None:
        if self._location_repository.get(location_id) is None:
            raise KeyError("Location not found")
        building = self._building_repository.get(building_id)
        if building is None or building.location_id != location_id:
            raise KeyError("Building not found")
        zone = self._zone_repository.get(zone_id)
        if zone is None or zone.building_id != building_id:
            raise KeyError("Zone not found")

    def _get_area(self, area_id: str) -> Area:
        area = self._area_repository.get(area_id)
        if area is None:
            raise KeyError("Area not found")
        return area

    @staticmethod
    def _to_response(area: Area) -> AreaResponse:
        return AreaResponse(id=area.id, name=area.name, zone_id=area.zone_id)
