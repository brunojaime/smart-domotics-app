from __future__ import annotations

from uuid import uuid4

from ..models import Room, RoomCreate, RoomUpdate
from ..repositories import BuildingRepository, RoomRepository


class RoomService:
    def __init__(self, building_repository: BuildingRepository, room_repository: RoomRepository) -> None:
        self._building_repository = building_repository
        self._room_repository = room_repository

    def list_rooms(self, location_id: str, building_id: str) -> list[Room]:
        self._ensure_building_exists(location_id, building_id)
        return self._room_repository.list_by_building(building_id)

    def create_room(self, location_id: str, building_id: str, data: RoomCreate) -> Room:
        self._ensure_building_exists(location_id, building_id)
        room = Room(id=str(uuid4()), name=data.name, building_id=building_id)
        return self._room_repository.add(room)

    def get_room(self, location_id: str, building_id: str, room_id: str) -> Room:
        self._ensure_building_exists(location_id, building_id)
        room = self._room_repository.get(building_id, room_id)
        if room is None:
            raise KeyError("Room not found")
        return room

    def update_room(self, location_id: str, building_id: str, room_id: str, data: RoomUpdate) -> Room:
        room = self.get_room(location_id, building_id, room_id)
        if data.name is None:
            raise ValueError("No updates provided")
        updated = room.model_copy(update={"name": data.name})
        return self._room_repository.update(updated)

    def delete_room(self, location_id: str, building_id: str, room_id: str) -> None:
        self._ensure_building_exists(location_id, building_id)
        self._room_repository.delete(building_id, room_id)

    def delete_rooms_for_building(self, building_id: str) -> None:
        self._room_repository.delete_by_building(building_id)

    def _ensure_building_exists(self, location_id: str, building_id: str) -> None:
        if self._building_repository.get(location_id, building_id) is None:
            raise KeyError("Building not found")
