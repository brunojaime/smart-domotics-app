from typing import List

from pydantic import BaseModel, Field


class LocationCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)


class LocationResponse(BaseModel):
    id: str
    name: str
    building_ids: List[str] = []


class BuildingCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)
    location_id: str = Field(..., min_length=1)


class BuildingResponse(BaseModel):
    id: str
    name: str
    location_id: str
    zone_ids: List[str] = []


class ZoneCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)
    building_id: str = Field(..., min_length=1)


class ZoneResponse(BaseModel):
    id: str
    name: str
    building_id: str
    area_ids: List[str] = []


class AreaCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)
    zone_id: str = Field(..., min_length=1)


class AreaResponse(BaseModel):
    id: str
    name: str
    zone_id: str
