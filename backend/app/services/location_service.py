from __future__ import annotations

from uuid import uuid4

from ..models import Location, LocationCreate, LocationUpdate
from ..repositories import LocationRepository


class LocationService:
    def __init__(self, repository: LocationRepository) -> None:
        self._repository = repository

    def list_locations(self) -> list[Location]:
        return self._repository.list()

    def create_location(self, data: LocationCreate) -> Location:
        location = Location(id=str(uuid4()), name=data.name)
        return self._repository.add(location)

    def get_location(self, location_id: str) -> Location:
        location = self._repository.get(location_id)
        if location is None:
            raise KeyError("Location not found")
        return location

    def update_location(self, location_id: str, data: LocationUpdate) -> Location:
        location = self.get_location(location_id)
        if data.name is None:
            raise ValueError("No updates provided")
        updated = location.model_copy(update={"name": data.name})
        return self._repository.update(updated)

    def delete_location(self, location_id: str) -> None:
        self._repository.delete(location_id)
