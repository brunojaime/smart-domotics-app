from dataclasses import dataclass, field
from typing import List


@dataclass
class Area:
    id: str
    name: str
    zone_id: str


@dataclass
class Zone:
    id: str
    name: str
    building_id: str
    areas: List[Area] = field(default_factory=list)


@dataclass
class Building:
    id: str
    name: str
    location_id: str
    zones: List[Zone] = field(default_factory=list)


@dataclass
class Location:
    id: str
    name: str
    buildings: List[Building] = field(default_factory=list)
