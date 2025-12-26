from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .config import settings
from .models import (
    DeviceCreateRequest,
    DeviceListResponse,
    DeviceResponse,
    MQTTCredentialsResponse,
    User,
)
from .services.hivemq_client import build_mqtt_credentials, device_topics
from .routers import buildings, locations, rooms
from .store import device_store

security = HTTPBearer(auto_error=False)
app = FastAPI(title="Smart Domotics Broker API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

api_prefix = "/api/v1"

app.include_router(locations.router, prefix=api_prefix)
app.include_router(buildings.router, prefix=api_prefix)
app.include_router(rooms.router, prefix=api_prefix)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security),
) -> User:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing token")

    token = credentials.credentials
    if not token.startswith(settings.app_token_prefix):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

    user_id = token.removeprefix(settings.app_token_prefix)
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token payload")
    return User(id=user_id)


@app.post("/api/auth/mqtt", response_model=MQTTCredentialsResponse)
async def issue_mqtt_credentials(
    user: User = Depends(get_current_user),
) -> MQTTCredentialsResponse:
    creds = build_mqtt_credentials(user.id)
    return MQTTCredentialsResponse(**creds)


@app.post("/api/devices", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
async def register_device(
    request: DeviceCreateRequest, user: User = Depends(get_current_user)
) -> DeviceResponse:
    try:
        device = device_store.create_device(user.id, request.device_id, request.name)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

    topics = device_topics(user.id, device.id)
    return DeviceResponse(device_id=device.id, name=device.name, topics=topics)


@app.get("/api/devices", response_model=DeviceListResponse)
async def list_devices(user: User = Depends(get_current_user)) -> DeviceListResponse:
    devices = [
        DeviceResponse(device_id=d.id, name=d.name, topics=device_topics(user.id, d.id))
        for d in device_store.list_devices(user.id)
    ]
    return DeviceListResponse(devices=devices)


@app.delete("/api/devices/{device_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_device(device_id: str, user: User = Depends(get_current_user)) -> None:
    try:
        device_store.delete_device(user.id, device_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Device not found") from exc


@app.get("/healthz")
async def health() -> dict:
    return {"status": "ok"}
