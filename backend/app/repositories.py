from __future__ import annotations

from typing import Dict, List

from .models import Building, Location, Room


class LocationRepository:
    def __init__(self) -> None:
        self._locations: Dict[str, Location] = {}

    def add(self, location: Location) -> Location:
        if location.id in self._locations:
            raise ValueError("Location already exists")
        self._locations[location.id] = location
        return location

    def list(self) -> List[Location]:
        return list(self._locations.values())

    def get(self, location_id: str) -> Location | None:
        return self._locations.get(location_id)

    def update(self, location: Location) -> Location:
        if location.id not in self._locations:
            raise KeyError("Location not found")
        self._locations[location.id] = location
        return location

    def delete(self, location_id: str) -> None:
        if location_id not in self._locations:
            raise KeyError("Location not found")
        del self._locations[location_id]

    def clear(self) -> None:
        self._locations.clear()


class BuildingRepository:
    def __init__(self) -> None:
        self._buildings: Dict[str, Dict[str, Building]] = {}

    def add(self, building: Building) -> Building:
        location_buildings = self._buildings.setdefault(building.location_id, {})
        if building.id in location_buildings:
            raise ValueError("Building already exists")
        location_buildings[building.id] = building
        return building

    def list_by_location(self, location_id: str) -> List[Building]:
        return list(self._buildings.get(location_id, {}).values())

    def get(self, location_id: str, building_id: str) -> Building | None:
        return self._buildings.get(location_id, {}).get(building_id)

    def update(self, building: Building) -> Building:
        location_buildings = self._buildings.get(building.location_id, {})
        if building.id not in location_buildings:
            raise KeyError("Building not found")
        location_buildings[building.id] = building
        return building

    def delete(self, location_id: str, building_id: str) -> None:
        location_buildings = self._buildings.get(location_id, {})
        if building_id not in location_buildings:
            raise KeyError("Building not found")
        del location_buildings[building_id]

    def clear(self) -> None:
        self._buildings.clear()


class RoomRepository:
    def __init__(self) -> None:
        self._rooms: Dict[str, Dict[str, Room]] = {}

    def add(self, room: Room) -> Room:
        building_rooms = self._rooms.setdefault(room.building_id, {})
        if room.id in building_rooms:
            raise ValueError("Room already exists")
        building_rooms[room.id] = room
        return room

    def list_by_building(self, building_id: str) -> List[Room]:
        return list(self._rooms.get(building_id, {}).values())

    def get(self, building_id: str, room_id: str) -> Room | None:
        return self._rooms.get(building_id, {}).get(room_id)

    def update(self, room: Room) -> Room:
        building_rooms = self._rooms.get(room.building_id, {})
        if room.id not in building_rooms:
            raise KeyError("Room not found")
        building_rooms[room.id] = room
        return room

    def delete(self, building_id: str, room_id: str) -> None:
        building_rooms = self._rooms.get(building_id, {})
        if room_id not in building_rooms:
            raise KeyError("Room not found")
        del building_rooms[room_id]

    def delete_by_building(self, building_id: str) -> None:
        self._rooms.pop(building_id, None)

    def clear(self) -> None:
        self._rooms.clear()
