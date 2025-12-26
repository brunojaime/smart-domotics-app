from __future__ import annotations

from typing import Dict, List

from .models import Building, Location, Room, Zone, Device


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


class ZoneRepository:
    def __init__(self) -> None:
        self._zones: Dict[str, Dict[str, Zone]] = {}

    def add(self, zone: Zone) -> Zone:
        building_zones = self._zones.setdefault(zone.building_id, {})
        if zone.id in building_zones:
            raise ValueError("Zone already exists")
        building_zones[zone.id] = zone
        return zone

    def list_by_building(self, building_id: str) -> List[Zone]:
        return list(self._zones.get(building_id, {}).values())

    def get(self, building_id: str, zone_id: str) -> Zone | None:
        return self._zones.get(building_id, {}).get(zone_id)

    def update(self, zone: Zone) -> Zone:
        building_zones = self._zones.get(zone.building_id, {})
        if zone.id not in building_zones:
            raise KeyError("Zone not found")
        building_zones[zone.id] = zone
        return zone

    def delete(self, building_id: str, zone_id: str) -> None:
        building_zones = self._zones.get(building_id, {})
        if zone_id not in building_zones:
            raise KeyError("Zone not found")
        del building_zones[zone_id]

    def delete_by_building(self, building_id: str) -> None:
        self._zones.pop(building_id, None)

    def clear(self) -> None:
        self._zones.clear()


class ZoneDeviceRepository:
    def __init__(self) -> None:
        self._devices: Dict[str, Dict[str, Device]] = {}

    def add(self, device: Device) -> Device:
        zone_devices = self._devices.setdefault(device.zone_id or "", {})
        if device.id in zone_devices:
            raise ValueError("Device already exists")
        zone_devices[device.id] = device
        return device

    def list_by_zone(self, zone_id: str) -> List[Device]:
        return list(self._devices.get(zone_id, {}).values())

    def get(self, zone_id: str, device_id: str) -> Device | None:
        return self._devices.get(zone_id, {}).get(device_id)

    def delete(self, zone_id: str, device_id: str) -> None:
        zone_devices = self._devices.get(zone_id, {})
        if device_id not in zone_devices:
            raise KeyError("Device not found")
        del zone_devices[device_id]

    def delete_by_zone(self, zone_id: str) -> None:
        self._devices.pop(zone_id, None)

    def clear(self) -> None:
        self._devices.clear()
