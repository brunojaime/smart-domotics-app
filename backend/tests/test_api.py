import pytest


def test_health(api_client) -> None:
import httpx
import pytest
from httpx import ASGITransport


def override_env() -> None:
    os.environ.update(
        {
            "APP_TOKEN_PREFIX": "user_",
            "APP_JWT_SECRET": "test_jwt_secret",
            "APP_JWT_EXPIRY_SECONDS": "3600",
            "APP_DATABASE_BACKEND": "memory",
            "APP_OAUTH_CLIENT_IDS": "{}",
            "HIVEMQ_HOST": "localhost",
            "HIVEMQ_PORT": "1883",
            "HIVEMQ_USERNAME": "local_backend",
            "HIVEMQ_PASSWORD": "local_backend_password",
            "MQTT_CREDENTIALS_TTL": "86400",
            "CORS_ALLOWED_ORIGINS": "http://localhost:3000,http://localhost:5173",
            "APP_JWT_SECRET": "test_secret",
            "USER_REPOSITORY": "memory",
        }
    )


def client() -> httpx.AsyncClient:
    override_env()
    import backend.app.settings as config
    import backend.app.config as config
    import backend.app.auth as auth

    importlib.reload(config)
    importlib.reload(auth)
    import backend.app.main as main

    importlib.reload(main)
    transport = ASGITransport(app=main.app, lifespan="on")
    return httpx.AsyncClient(transport=transport, base_url="http://test")


@pytest.fixture
async def api_client() -> AsyncGenerator[httpx.AsyncClient, None]:
    c = client()
    try:
        yield c
    finally:
        await c.aclose()
        try:
            from backend.app.main import app

            device_repository = app.state.device_repository
            if hasattr(device_repository, "clear"):
                device_repository.clear()
        except Exception:
            pass
        device_store.clear()
from fastapi.testclient import TestClient


async def register_and_login(api_client: httpx.AsyncClient, username: str = "alice") -> dict:
    reg_resp = await api_client.post(
        "/api/auth/register",
        json={"username": username, "password": "secret123", "email": f"{username}@example.com"},
    )
    assert reg_resp.status_code == 201
    tokens = reg_resp.json()
    profile_resp = await api_client.get(
        "/api/profile", headers={"Authorization": f"Bearer {tokens['access_token']}"}
    )
    assert profile_resp.status_code == 200
    user_info = profile_resp.json()
    return {
        "Authorization": f"Bearer {tokens['access_token']}",
        "refresh": tokens["refresh_token"],
        "user_id": user_info["id"],
    }


def test_health(api_client: TestClient) -> None:
    response = api_client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_issue_mqtt_credentials(api_client, auth_header) -> None:
    response = api_client.post("/api/auth/mqtt", headers=auth_header)
@pytest.mark.anyio
async def test_authentication_and_profile(api_client: httpx.AsyncClient) -> None:
    headers = await register_and_login(api_client)

    login_resp = await api_client.post(
        "/api/auth/login",
        data={"username": "alice", "password": "secret123"},
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    assert login_resp.status_code == 200
    tokens = login_resp.json()
    assert tokens["access_token"]

    profile_resp = await api_client.get(
        "/api/profile", headers={"Authorization": f"Bearer {tokens['access_token']}"}
    )
    assert profile_resp.status_code == 200
    assert profile_resp.json()["username"] == "alice"

    refresh_resp = await api_client.post("/api/auth/refresh", json={"refresh_token": tokens["refresh_token"]})
    assert refresh_resp.status_code == 200
    assert refresh_resp.json()["access_token"]


@pytest.mark.anyio
async def test_issue_mqtt_credentials(api_client: httpx.AsyncClient) -> None:
    headers = await register_and_login(api_client)
    response = await api_client.post("/api/auth/mqtt", headers={"Authorization": headers["Authorization"]})
def test_issue_mqtt_credentials(api_client: TestClient) -> None:
    response = api_client.post("/api/auth/mqtt", headers=AUTH_HEADER)
    assert response.status_code == 200
    body = response.json()
    assert body["host"] == "localhost"
    assert body["username"] == "local_backend"
    assert body["client_id"].startswith("alice-")
    assert body["topics"] == [f"users/{headers['user_id']}/devices/#"]
    assert body["expires_in_seconds"] == 86400


def test_device_provisioning_flow(api_client, auth_header, sample_device_payload) -> None:
    create_resp = api_client.post("/api/devices", headers=auth_header, json=sample_device_payload)
    assert create_resp.status_code == 201
    body = create_resp.json()
    assert body["device_id"] == sample_device_payload["device_id"]
    assert body["topics"] == ["users/alice/devices/lamp-1/#"]

    list_resp = api_client.get("/api/devices", headers=auth_header)
@pytest.mark.anyio
async def test_device_provisioning_flow(api_client: httpx.AsyncClient) -> None:
    headers = await register_and_login(api_client)
    auth_header = {"Authorization": headers["Authorization"]}
    create_resp = await api_client.post(
        "/api/devices", headers=auth_header, json={"device_id": "lamp-1", "name": "Lamp"}
def test_device_provisioning_flow(api_client: TestClient) -> None:
    create_resp = api_client.post(
        "/api/devices", headers=AUTH_HEADER, json={"device_id": "lamp-1", "name": "Lamp"}
    )
    assert create_resp.status_code == 201
    body = create_resp.json()
    assert body["device_id"] == "lamp-1"
    assert body["topics"] == [f"users/{headers['user_id']}/devices/lamp-1/#"]

    list_resp = await api_client.get("/api/devices", headers=auth_header)
    list_resp = api_client.get("/api/devices", headers=AUTH_HEADER)
    assert list_resp.status_code == 200
    devices = list_resp.json()["devices"]
    assert len(devices) == 1
    assert devices[0]["name"] == sample_device_payload["name"]

    delete_resp = api_client.delete("/api/devices/lamp-1", headers=auth_header)
    assert delete_resp.status_code == 204

    list_resp_after = api_client.get("/api/devices", headers=auth_header)
    delete_resp = await api_client.delete("/api/devices/lamp-1", headers=auth_header)
    assert delete_resp.status_code == 204

    list_resp_after = await api_client.get("/api/devices", headers=auth_header)
    delete_resp = api_client.delete("/api/devices/lamp-1", headers=AUTH_HEADER)
    assert delete_resp.status_code == 204

    list_resp_after = api_client.get("/api/devices", headers=AUTH_HEADER)
    assert list_resp_after.status_code == 200
    assert list_resp_after.json()["devices"] == []


def test_duplicate_device_rejected(api_client, auth_header) -> None:
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = api_client.post("/api/devices", headers=auth_header, json=payload)
    assert first.status_code == 201

    duplicate = api_client.post("/api/devices", headers=auth_header, json=payload)
@pytest.mark.anyio
async def test_duplicate_device_rejected(api_client: httpx.AsyncClient) -> None:
    headers = await register_and_login(api_client)
    auth_header = {"Authorization": headers["Authorization"]}
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = await api_client.post("/api/devices", headers=auth_header, json=payload)
    assert first.status_code == 201

    duplicate = await api_client.post("/api/devices", headers=auth_header, json=payload)
def test_duplicate_device_rejected(api_client: TestClient) -> None:
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = api_client.post("/api/devices", headers=AUTH_HEADER, json=payload)
    assert first.status_code == 201

    duplicate = api_client.post("/api/devices", headers=AUTH_HEADER, json=payload)
    assert duplicate.status_code == 400
    assert "already exists" in duplicate.json()["detail"]


@pytest.mark.parametrize("token", [None, "", "otherprefix_user"])
def test_auth_required(api_client, token: str | None) -> None:
@pytest.mark.parametrize("token", [None, "", "invalid.jwt.token"])
@pytest.mark.anyio
async def test_auth_required(api_client: httpx.AsyncClient, token: str | None) -> None:
@pytest.mark.parametrize("token", [None, "", "otherprefix_user"])
def test_auth_required(api_client: TestClient, token: str | None) -> None:
    headers = {"Authorization": f"Bearer {token}"} if token is not None else {}
    resp = api_client.get("/api/devices", headers=headers)
    assert resp.status_code == 401
