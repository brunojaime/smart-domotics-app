from datetime import datetime, timezone
from typing import List

from pydantic import BaseModel, Field


class User(BaseModel):
    id: str


class Device(BaseModel):
    id: str
    name: str
    owner_id: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class DeviceCreateRequest(BaseModel):
    device_id: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1)


class DeviceResponse(BaseModel):
    device_id: str
    name: str
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
