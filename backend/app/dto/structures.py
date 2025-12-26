from typing import List

from pydantic import BaseModel, Field


class LocationCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)


class LocationResponse(BaseModel):
    id: str
    name: str
    building_ids: List[str] = []


class LocationUpdateRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class BuildingCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)


class BuildingResponse(BaseModel):
    id: str
    name: str
    location_id: str
    zone_ids: List[str] = []


class BuildingUpdateRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class ZoneCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)


class ZoneResponse(BaseModel):
    id: str
    name: str
    building_id: str
    area_ids: List[str] = []


class ZoneUpdateRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class AreaCreateRequest(BaseModel):
    name: str = Field(..., min_length=1)


class AreaResponse(BaseModel):
    id: str
    name: str
    zone_id: str


class AreaUpdateRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1)
