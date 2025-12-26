import importlib
import os
from typing import AsyncGenerator

import pytest
import httpx
from httpx import ASGITransport

from backend.app.store import device_store


def override_env() -> None:
    os.environ.update(
        {
            "APP_TOKEN_PREFIX": "user_",
            "HIVEMQ_HOST": "localhost",
            "HIVEMQ_PORT": "1883",
            "HIVEMQ_USERNAME": "local_backend",
            "HIVEMQ_PASSWORD": "local_backend_password",
            "MQTT_CREDENTIALS_TTL": "86400",
            "CORS_ALLOWED_ORIGINS": "http://localhost:3000,http://localhost:5173",
        }
    )


def client() -> httpx.AsyncClient:
    override_env()
    import backend.app.config as config

    importlib.reload(config)
    import backend.app.main as main

    importlib.reload(main)
    transport = ASGITransport(app=main.app)
    return httpx.AsyncClient(transport=transport, base_url="http://test")


@pytest.fixture
async def api_client() -> AsyncGenerator[httpx.AsyncClient, None]:
    c = client()
    try:
        yield c
    finally:
        await c.aclose()
        device_store.clear()


AUTH_HEADER = {"Authorization": "Bearer user_alice"}


@pytest.mark.anyio
async def test_health(api_client: httpx.AsyncClient) -> None:
    response = await api_client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


@pytest.mark.anyio
async def test_issue_mqtt_credentials(api_client: httpx.AsyncClient) -> None:
    response = await api_client.post("/api/auth/mqtt", headers=AUTH_HEADER)
    assert response.status_code == 200
    body = response.json()
    assert body["host"] == "localhost"
    assert body["username"] == "local_backend"
    assert body["client_id"].startswith("alice-")
    assert body["topics"] == ["users/alice/devices/#"]
    assert body["expires_in_seconds"] == 86400


@pytest.mark.anyio
async def test_device_provisioning_flow(api_client: httpx.AsyncClient) -> None:
    create_resp = await api_client.post(
        "/api/devices", headers=AUTH_HEADER, json={"device_id": "lamp-1", "name": "Lamp"}
    )
    assert create_resp.status_code == 201
    body = create_resp.json()
    assert body["device_id"] == "lamp-1"
    assert body["topics"] == ["users/alice/devices/lamp-1/#"]

    list_resp = await api_client.get("/api/devices", headers=AUTH_HEADER)
    assert list_resp.status_code == 200
    devices = list_resp.json()["devices"]
    assert len(devices) == 1
    assert devices[0]["name"] == "Lamp"

    delete_resp = await api_client.delete("/api/devices/lamp-1", headers=AUTH_HEADER)
    assert delete_resp.status_code == 204

    list_resp_after = await api_client.get("/api/devices", headers=AUTH_HEADER)
    assert list_resp_after.status_code == 200
    assert list_resp_after.json()["devices"] == []


@pytest.mark.anyio
async def test_duplicate_device_rejected(api_client: httpx.AsyncClient) -> None:
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = await api_client.post("/api/devices", headers=AUTH_HEADER, json=payload)
    assert first.status_code == 201

    duplicate = await api_client.post("/api/devices", headers=AUTH_HEADER, json=payload)
    assert duplicate.status_code == 400
    assert "already exists" in duplicate.json()["detail"]


@pytest.mark.parametrize("token", [None, "", "otherprefix_user"])
@pytest.mark.anyio
async def test_auth_required(api_client: httpx.AsyncClient, token: str | None) -> None:
    headers = {"Authorization": f"Bearer {token}"} if token is not None else {}
    resp = await api_client.get("/api/devices", headers=headers)
    assert resp.status_code == 401
