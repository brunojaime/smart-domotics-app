import secrets

from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordRequestForm
import httpx

from .auth import (
    create_access_token,
    create_refresh_token,
    create_user,
    get_current_user,
    user_repo,
    verify_password,
)
from .config import settings
from .models import (
    DeviceCreateRequest,
    DeviceListResponse,
    DeviceResponse,
    MQTTCredentialsResponse,
    RefreshRequest,
    TokenResponse,
    User,
    UserCreateRequest,
)
from .services.hivemq_client import build_mqtt_credentials, device_topics
from .store import device_store

app = FastAPI(title="Smart Domotics Broker API", version="0.2.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/api/auth/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register_user(request: UserCreateRequest) -> TokenResponse:
    try:
        user = create_user(request.username, request.password, request.email)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

    access_token = create_access_token(user.id)
    refresh_token = create_refresh_token(user.id)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@app.post("/api/auth/login", response_model=TokenResponse)
async def login(form_data: OAuth2PasswordRequestForm = Depends()) -> TokenResponse:
    user = user_repo.get_by_username(form_data.username)
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

    return TokenResponse(
        access_token=create_access_token(user.id), refresh_token=create_refresh_token(user.id)
    )


@app.post("/api/auth/refresh", response_model=TokenResponse)
async def refresh_token(request: RefreshRequest) -> TokenResponse:
    from jose import jwt
    from jose.exceptions import JWTError

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
        access_token=create_access_token(user_id), refresh_token=create_refresh_token(user_id)
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
