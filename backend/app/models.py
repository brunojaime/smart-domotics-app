from datetime import datetime, timezone
from typing import List, Optional

from pydantic import BaseModel, Field, validator


class User(BaseModel):
    id: str
    username: str
    email: Optional[str] = None


class UserInDB(User):
    hashed_password: str
    google_sub: Optional[str] = None


class UserCreateRequest(BaseModel):
    username: str = Field(..., min_length=3)
    password: str = Field(..., min_length=6)
    email: Optional[str] = None


class RegisterRequest(BaseModel):
    username: Optional[str] = None
    name: Optional[str] = None
    email: str = Field(..., min_length=3)
    password: str = Field(..., min_length=6)

    @validator("username", "name", pre=True)
    def blank_to_none(cls, value: Optional[str]) -> Optional[str]:
        if value is None:
            return None
        value = value.strip()
        return value or None


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshRequest(BaseModel):
    refresh_token: str


class Device(BaseModel):
    id: str
    name: str
    owner_id: str | None = None
    zone_id: str | None = None
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class DeviceCreateRequest(BaseModel):
    device_id: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1)
    zone_id: str | None = None


class DeviceResponse(BaseModel):
    device_id: str
    name: str
    zone_id: str | None = None
    topics: List[str]


class DeviceListResponse(BaseModel):
    devices: List[DeviceResponse]


class MQTTCredentialsResponse(BaseModel):
    host: str
    port: int
    username: str
    password: str
    client_id: str
    topics: List[str]
    expires_in_seconds: int


class Location(BaseModel):
    id: str
    name: str


class LocationCreate(BaseModel):
    name: str = Field(..., min_length=1)


class LocationUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class LocationResponse(Location):
    pass


class Building(BaseModel):
    id: str
    name: str
    location_id: str


class BuildingCreate(BaseModel):
    name: str = Field(..., min_length=1)


class BuildingUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class BuildingResponse(Building):
    pass


class Room(BaseModel):
    id: str
    name: str
    building_id: str


class RoomCreate(BaseModel):
    name: str = Field(..., min_length=1)


class RoomUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class RoomResponse(Room):
    pass


class Zone(BaseModel):
    id: str
    name: str
    building_id: str


class ZoneCreate(BaseModel):
    name: str = Field(..., min_length=1)


class ZoneUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1)


class ZoneResponse(Zone):
    pass


class ZoneDeviceCreate(BaseModel):
    device_id: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1)


class ZoneDeviceResponse(BaseModel):
    device_id: str
    name: str
    zone_id: str
