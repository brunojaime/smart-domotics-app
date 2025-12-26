from __future__ import annotations

from ..domain.entities import Location
from ..dto.structures import LocationCreateRequest, LocationResponse, LocationUpdateRequest
from ..repositories.base import LocationRepository


class LocationService:
    def __init__(self, repository: LocationRepository) -> None:
        self._repository = repository

    def list_locations(self) -> list[LocationResponse]:
        return [self._to_response(location) for location in self._repository.list()]

    def create_location(self, data: LocationCreateRequest) -> LocationResponse:
        location = self._repository.create(data.name)
        return self._to_response(location)

    def get_location(self, location_id: str) -> LocationResponse:
        location = self._repository.get(location_id)
        if location is None:
            raise KeyError("Location not found")
        return self._to_response(location)

    def update_location(self, location_id: str, data: LocationUpdateRequest) -> LocationResponse:
        location = self._repository.get(location_id)
        if location is None:
            raise KeyError("Location not found")
        if data.name is None:
            raise ValueError("No updates provided")
        updated = Location(id=location.id, name=data.name, buildings=location.buildings)
        return self._to_response(self._repository.update(updated))

    def delete_location(self, location_id: str) -> None:
        self._repository.delete(location_id)

    @staticmethod
    def _to_response(location: Location) -> LocationResponse:
        return LocationResponse(
            id=location.id,
            name=location.name,
            building_ids=[building.id for building in location.buildings],
        )
