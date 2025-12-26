from __future__ import annotations

from typing import Dict
from uuid import uuid4

from ..domain.entities import Area, Building, Location, Zone
from .base import AreaRepository, BuildingRepository, LocationRepository, RepositoryProvider, ZoneRepository


class InMemoryDataStore:
    def __init__(self) -> None:
        self.locations: Dict[str, Location] = {}
        self.buildings: Dict[str, Building] = {}
        self.zones: Dict[str, Zone] = {}
        self.areas: Dict[str, Area] = {}

    def clear(self) -> None:
        self.locations.clear()
        self.buildings.clear()
        self.zones.clear()
        self.areas.clear()


class InMemoryLocationRepository(LocationRepository):
    def __init__(self, store: InMemoryDataStore) -> None:
        self._store = store

    def create(self, name: str) -> Location:
        location_id = str(uuid4())
        location = Location(id=location_id, name=name)
        self._store.locations[location_id] = location
        return location

    def get(self, location_id: str) -> Location | None:
        return self._store.locations.get(location_id)

    def list(self) -> list[Location]:
        return list(self._store.locations.values())

    def update(self, location: Location) -> Location:
        if location.id not in self._store.locations:
            raise KeyError("Location not found")
        self._store.locations[location.id] = location
        return location

    def delete(self, location_id: str) -> None:
        if location_id not in self._store.locations:
            raise KeyError("Location not found")
        building_ids = [b.id for b in self._store.buildings.values() if b.location_id == location_id]
        for building_id in building_ids:
            InMemoryBuildingRepository(self._store).delete(building_id)
        del self._store.locations[location_id]


class InMemoryBuildingRepository(BuildingRepository):
    def __init__(self, store: InMemoryDataStore) -> None:
        self._store = store

    def create(self, name: str, location_id: str) -> Building:
        if location_id not in self._store.locations:
            raise ValueError("Location not found")
        building_id = str(uuid4())
        building = Building(id=building_id, name=name, location_id=location_id)
        self._store.buildings[building_id] = building
        self._store.locations[location_id].buildings.append(building)
        return building

    def get(self, building_id: str) -> Building | None:
        return self._store.buildings.get(building_id)

    def list_for_location(self, location_id: str) -> list[Building]:
        return [b for b in self._store.buildings.values() if b.location_id == location_id]

    def update(self, building: Building) -> Building:
        if building.id not in self._store.buildings:
            raise KeyError("Building not found")
        self._store.buildings[building.id] = building
        location = self._store.locations.get(building.location_id)
        if location:
            location.buildings = [b for b in location.buildings if b.id != building.id] + [building]
        return building

    def delete(self, building_id: str) -> None:
        building = self._store.buildings.get(building_id)
        if building is None:
            raise KeyError("Building not found")
        zone_ids = [z.id for z in self._store.zones.values() if z.building_id == building_id]
        for zone_id in zone_ids:
            InMemoryZoneRepository(self._store).delete(zone_id)
        location = self._store.locations.get(building.location_id)
        if location:
            location.buildings = [b for b in location.buildings if b.id != building_id]
        del self._store.buildings[building_id]


class InMemoryZoneRepository(ZoneRepository):
    def __init__(self, store: InMemoryDataStore) -> None:
        self._store = store

    def create(self, name: str, building_id: str) -> Zone:
        if building_id not in self._store.buildings:
            raise ValueError("Building not found")
        zone_id = str(uuid4())
        zone = Zone(id=zone_id, name=name, building_id=building_id)
        self._store.zones[zone_id] = zone
        building = self._store.buildings[building_id]
        building.zones.append(zone)
        return zone

    def get(self, zone_id: str) -> Zone | None:
        return self._store.zones.get(zone_id)

    def list_for_building(self, building_id: str) -> list[Zone]:
        return [z for z in self._store.zones.values() if z.building_id == building_id]

    def update(self, zone: Zone) -> Zone:
        if zone.id not in self._store.zones:
            raise KeyError("Zone not found")
        self._store.zones[zone.id] = zone
        building = self._store.buildings.get(zone.building_id)
        if building:
            building.zones = [z for z in building.zones if z.id != zone.id] + [zone]
        return zone

    def delete(self, zone_id: str) -> None:
        zone = self._store.zones.get(zone_id)
        if zone is None:
            raise KeyError("Zone not found")
        area_ids = [a.id for a in self._store.areas.values() if a.zone_id == zone_id]
        for area_id in area_ids:
            InMemoryAreaRepository(self._store).delete(area_id)
        building = self._store.buildings.get(zone.building_id)
        if building:
            building.zones = [z for z in building.zones if z.id != zone_id]
        del self._store.zones[zone_id]


class InMemoryAreaRepository(AreaRepository):
    def __init__(self, store: InMemoryDataStore) -> None:
        self._store = store

    def create(self, name: str, zone_id: str) -> Area:
        if zone_id not in self._store.zones:
            raise ValueError("Zone not found")
        area_id = str(uuid4())
        area = Area(id=area_id, name=name, zone_id=zone_id)
        self._store.areas[area_id] = area
        zone = self._store.zones[zone_id]
        zone.areas.append(area)
        return area

    def get(self, area_id: str) -> Area | None:
        return self._store.areas.get(area_id)

    def list_for_zone(self, zone_id: str) -> list[Area]:
        return [a for a in self._store.areas.values() if a.zone_id == zone_id]

    def update(self, area: Area) -> Area:
        if area.id not in self._store.areas:
            raise KeyError("Area not found")
        self._store.areas[area.id] = area
        zone = self._store.zones.get(area.zone_id)
        if zone:
            zone.areas = [a for a in zone.areas if a.id != area.id] + [area]
        return area

    def delete(self, area_id: str) -> None:
        area = self._store.areas.get(area_id)
        if area is None:
            raise KeyError("Area not found")
        zone = self._store.zones.get(area.zone_id)
        if zone:
            zone.areas = [a for a in zone.areas if a.id != area_id]
        del self._store.areas[area_id]


class InMemoryRepositoryProvider:
    def __init__(self) -> None:
        self._store = InMemoryDataStore()
        self.locations = InMemoryLocationRepository(self._store)
        self.buildings = InMemoryBuildingRepository(self._store)
        self.zones = InMemoryZoneRepository(self._store)
        self.areas = InMemoryAreaRepository(self._store)

    def clear(self) -> None:
        self._store.clear()


def create_in_memory_provider() -> RepositoryProvider:
    return InMemoryRepositoryProvider()
