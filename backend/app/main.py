import inspect
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException, Request, status
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
from .repositories.device_repository import DeviceRepository, InMemoryDeviceRepository
from .services.hivemq_client import build_mqtt_credentials, device_topics

security = HTTPBearer(auto_error=False)


def build_device_repository() -> DeviceRepository:
    if settings.database_backend == "memory":
        return InMemoryDeviceRepository()
    raise ValueError(f"Unsupported database backend: {settings.database_backend}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.settings = settings
    app.state.device_repository = build_device_repository()
    try:
        yield
    finally:
        repository = app.state.device_repository
        shutdown = getattr(repository, "close", None)
        if callable(shutdown):
            result = shutdown()
            if inspect.isawaitable(result):
                await result


def get_device_repository(request: Request) -> DeviceRepository:
    return request.app.state.device_repository


def get_current_user(
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


app = FastAPI(title="Smart Domotics Broker API", version="0.1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/api/auth/mqtt", response_model=MQTTCredentialsResponse)
async def issue_mqtt_credentials(
    user: User = Depends(get_current_user),
) -> MQTTCredentialsResponse:
    creds = build_mqtt_credentials(user.id)
    return MQTTCredentialsResponse(**creds)


@app.post("/api/devices", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
async def register_device(
    request: DeviceCreateRequest,
    user: User = Depends(get_current_user),
    device_repository: DeviceRepository = Depends(get_device_repository),
) -> DeviceResponse:
    try:
        device = device_repository.create_device(user.id, request.device_id, request.name)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

    topics = device_topics(user.id, device.id)
    return DeviceResponse(device_id=device.id, name=device.name, topics=topics)


@app.get("/api/devices", response_model=DeviceListResponse)
async def list_devices(
    user: User = Depends(get_current_user),
    device_repository: DeviceRepository = Depends(get_device_repository),
) -> DeviceListResponse:
    devices = [
        DeviceResponse(device_id=d.id, name=d.name, topics=device_topics(user.id, d.id))
        for d in device_repository.list_devices(user.id)
    ]
    return DeviceListResponse(devices=devices)


@app.delete("/api/devices/{device_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_device(
    device_id: str,
    user: User = Depends(get_current_user),
    device_repository: DeviceRepository = Depends(get_device_repository),
) -> None:
    try:
        device_repository.delete_device(user.id, device_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Device not found") from exc


@app.get("/healthz")
async def health() -> dict:
    return {"status": "ok"}
