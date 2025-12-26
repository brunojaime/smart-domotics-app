from __future__ import annotations

from abc import ABC, abstractmethod
from typing import List, Protocol

from ..domain.entities import Area, Building, Location, Zone


class LocationRepository(ABC):
    @abstractmethod
    def create(self, name: str) -> Location:
        raise NotImplementedError

    @abstractmethod
    def get(self, location_id: str) -> Location | None:
        raise NotImplementedError

    @abstractmethod
    def list(self) -> List[Location]:
        raise NotImplementedError

    @abstractmethod
    def delete(self, location_id: str) -> None:
        raise NotImplementedError


class BuildingRepository(ABC):
    @abstractmethod
    def create(self, name: str, location_id: str) -> Building:
        raise NotImplementedError

    @abstractmethod
    def get(self, building_id: str) -> Building | None:
        raise NotImplementedError

    @abstractmethod
    def list_for_location(self, location_id: str) -> List[Building]:
        raise NotImplementedError

    @abstractmethod
    def delete(self, building_id: str) -> None:
        raise NotImplementedError


class ZoneRepository(ABC):
    @abstractmethod
    def create(self, name: str, building_id: str) -> Zone:
        raise NotImplementedError

    @abstractmethod
    def get(self, zone_id: str) -> Zone | None:
        raise NotImplementedError

    @abstractmethod
    def list_for_building(self, building_id: str) -> List[Zone]:
        raise NotImplementedError

    @abstractmethod
    def delete(self, zone_id: str) -> None:
        raise NotImplementedError


class AreaRepository(ABC):
    @abstractmethod
    def create(self, name: str, zone_id: str) -> Area:
        raise NotImplementedError

    @abstractmethod
    def get(self, area_id: str) -> Area | None:
        raise NotImplementedError

    @abstractmethod
    def list_for_zone(self, zone_id: str) -> List[Area]:
        raise NotImplementedError

    @abstractmethod
    def delete(self, area_id: str) -> None:
        raise NotImplementedError


class RepositoryProvider(Protocol):
    locations: LocationRepository
    buildings: BuildingRepository
    zones: ZoneRepository
    areas: AreaRepository
