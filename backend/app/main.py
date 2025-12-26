import inspect
import secrets
from contextlib import asynccontextmanager

import httpx
from fastapi import Body, Depends, FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .auth import create_access_token, create_refresh_token, create_user, decode_token, user_repo, verify_password
from .config import settings
from .models import (
    DeviceCreateRequest,
    DeviceListResponse,
    DeviceResponse,
    MQTTCredentialsResponse,
    RegisterRequest,
    RefreshRequest,
    TokenResponse,
    User,
    UserCreateRequest,
)
from .repositories.device_repository import DeviceRepository, InMemoryDeviceRepository
from .routers import areas, buildings, locations, zones
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


def _user_from_prefix_token(token: str) -> User | None:
    prefix = settings.app_token_prefix
    if not token.startswith(prefix):
        return None
    user_id = token[len(prefix) :]
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
    return User(id=user_id, username=user_id, email=None)


def _user_from_jwt(token: str) -> User:
    payload = decode_token(token)
    if payload.get("type") != "access":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")
    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token payload")
    user = user_repo.get_by_id(user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return User(id=user.id, username=user.username, email=user.email)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security),
) -> User:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing token")

    token = credentials.credentials
    user = _user_from_prefix_token(token)
    if user:
        return user
    return _user_from_jwt(token)


app = FastAPI(title="Smart Domotics Broker API", version="0.2.0", lifespan=lifespan)

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
app.include_router(zones.router, prefix=api_prefix)
app.include_router(areas.router, prefix=api_prefix)


@app.get("/healthz")
async def healthcheck() -> dict:
    return {"status": "ok"}


@app.post("/api/auth/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register_user(request: RegisterRequest) -> TokenResponse:
    username = request.username or request.name or request.email.split("@", 1)[0]
    try:
        user = create_user(username, request.password, request.email)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(user.id)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


async def _get_login_payload(request: Request) -> tuple[str, str]:
    content_type = request.headers.get("content-type", "")
    if "application/json" in content_type:
        body = await request.json()
        email = body.get("email") or body.get("username")
        password = body.get("password")
        if email and password:
            return str(email), str(password)
    form = await request.form()
    username = form.get("username")
    password = form.get("password")
    if username and password:
        return str(username), str(password)
    raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Invalid login payload")


@app.post("/api/auth/login", response_model=TokenResponse)
async def login(payload: tuple[str, str] = Depends(_get_login_payload)) -> TokenResponse:
    identifier, password = payload
    user = user_repo.get_by_username(identifier) or user_repo.get_by_email(identifier)
    if not user or not verify_password(password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

    return TokenResponse(
        access_token=create_access_token(user.id),
        refresh_token=create_refresh_token(user.id),
    )


@app.post("/api/auth/refresh", response_model=TokenResponse)
async def refresh_token(request: RefreshRequest) -> TokenResponse:
    from jose import JWTError, jwt

    try:
        payload = jwt.decode(request.refresh_token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
    except JWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token") from exc

    if payload.get("type") != "refresh":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")

    user_id = payload.get("sub")
    if not user_id or not user_repo.get_by_id(user_id):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")

    return TokenResponse(
        access_token=create_access_token(user_id),
        refresh_token=create_refresh_token(user_id),
    )


@app.get("/api/profile", response_model=User)
async def profile(current_user: User = Depends(get_current_user)) -> User:
    return current_user


def _unique_username(base: str) -> str:
    candidate = base
    suffix = 1
    while user_repo.get_by_username(candidate):
        candidate = f"{base}{suffix}"
        suffix += 1
    return candidate


@app.get("/api/auth/google/callback", response_model=TokenResponse)
async def google_callback(code: str):
    if not settings.google_client_id or not settings.google_client_secret:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Google OAuth not configured")

    token_payload = {
        "code": code,
        "client_id": settings.google_client_id,
        "client_secret": settings.google_client_secret,
        "redirect_uri": settings.google_redirect_uri,
        "grant_type": "authorization_code",
    }

    async with httpx.AsyncClient() as client:
        token_resp = await client.post("https://oauth2.googleapis.com/token", data=token_payload)
        if token_resp.status_code != 200:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Google token exchange failed")
        token_data = token_resp.json()
        google_access_token = token_data.get("access_token")
        if not google_access_token:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Google token missing")

        userinfo_resp = await client.get(
            "https://www.googleapis.com/oauth2/v2/userinfo",
            headers={"Authorization": f"Bearer {google_access_token}"},
        )

        if userinfo_resp.status_code != 200:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Failed to fetch Google profile")
        userinfo = userinfo_resp.json()

    google_sub = userinfo.get("id")
    email = userinfo.get("email")
    name = userinfo.get("name") or email or "google_user"

    user = user_repo.get_by_google_sub(google_sub) if google_sub else None
    if not user:
        username_base = email.split("@", 1)[0] if email and "@" in email else google_sub or name
        username = _unique_username(username_base)
        generated_password = secrets.token_urlsafe(12)
        user = create_user(username=username, password=generated_password, email=email, google_sub=google_sub)

    return TokenResponse(access_token=create_access_token(user.id), refresh_token=create_refresh_token(user.id))


@app.post("/api/auth/google", response_model=TokenResponse)
async def google_sign_in(payload: dict = Body(...)) -> TokenResponse:
    id_token = payload.get("id_token")
    if not id_token:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Missing id_token")
    username = _unique_username("google_user")
    user = create_user(username=username, password=secrets.token_urlsafe(12), email=None, google_sub=id_token)
    return TokenResponse(access_token=create_access_token(user.id), refresh_token=create_refresh_token(user.id))


@app.post("/api/auth/oauth/callback", response_model=TokenResponse)
async def oauth_callback(payload: dict = Body(...)) -> TokenResponse:
    code = payload.get("code")
    if not code:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Missing code")
    return await google_callback(code)


@app.post("/api/auth/mqtt", response_model=MQTTCredentialsResponse)
async def issue_mqtt_credentials(user: User = Depends(get_current_user)) -> MQTTCredentialsResponse:
    creds = build_mqtt_credentials(user.id)
    creds["client_id"] = f"{user.username}-app"
    return MQTTCredentialsResponse(**creds)


@app.post("/api/devices", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
async def register_device(
    payload: DeviceCreateRequest,
    user: User = Depends(get_current_user),
    repo: DeviceRepository = Depends(get_device_repository),
) -> DeviceResponse:
    try:
        device = repo.create_device(user.id, payload.device_id, payload.name)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return DeviceResponse(
        device_id=device.id,
        name=device.name,
        topics=device_topics(user.id, device.id),
    )


@app.get("/api/devices", response_model=DeviceListResponse)
async def list_devices(
    user: User = Depends(get_current_user),
    repo: DeviceRepository = Depends(get_device_repository),
) -> DeviceListResponse:
    devices = [
        DeviceResponse(device_id=device.id, name=device.name, topics=device_topics(user.id, device.id))
        for device in repo.list_devices(user.id)
    ]
    return DeviceListResponse(devices=devices)


@app.delete("/api/devices/{device_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_device(
    device_id: str,
    user: User = Depends(get_current_user),
    repo: DeviceRepository = Depends(get_device_repository),
) -> None:
    try:
        repo.delete_device(user.id, device_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
