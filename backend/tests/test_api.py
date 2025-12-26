import pytest


@pytest.mark.anyio
async def test_health(async_api_client) -> None:
    response = await async_api_client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


async def _register_and_login(async_api_client, username: str = "alice") -> dict:
    reg_resp = await async_api_client.post(
        "/api/auth/register",
        json={"username": username, "password": "secret123", "email": f"{username}@example.com"},
    )
    assert reg_resp.status_code == 201
    tokens = reg_resp.json()
    profile_resp = await async_api_client.get(
        "/api/profile", headers={"Authorization": f"Bearer {tokens['access_token']}"}
    )
    assert profile_resp.status_code == 200
    user_info = profile_resp.json()
    return {
        "Authorization": f"Bearer {tokens['access_token']}",
        "refresh": tokens["refresh_token"],
        "user_id": user_info["id"],
    }


@pytest.mark.anyio
async def test_authentication_and_profile(async_api_client) -> None:
    headers = await _register_and_login(async_api_client)

    login_resp = await async_api_client.post(
        "/api/auth/login",
        data={"username": "alice", "password": "secret123"},
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    assert login_resp.status_code == 200
    tokens = login_resp.json()
    assert tokens["access_token"]

    profile_resp = await async_api_client.get(
        "/api/profile", headers={"Authorization": f"Bearer {tokens['access_token']}"}
    )
    assert profile_resp.status_code == 200
    assert profile_resp.json()["username"] == "alice"

    refresh_resp = await async_api_client.post("/api/auth/refresh", json={"refresh_token": tokens["refresh_token"]})
    assert refresh_resp.status_code == 200
    assert refresh_resp.json()["access_token"]

    assert headers["Authorization"].startswith("Bearer ")


@pytest.mark.anyio
async def test_issue_mqtt_credentials(async_api_client) -> None:
    headers = await _register_and_login(async_api_client)
    response = await async_api_client.post("/api/auth/mqtt", headers={"Authorization": headers["Authorization"]})
    assert response.status_code == 200
    body = response.json()
    assert body["host"] == "localhost"
    assert body["username"] == "local_backend"
    assert body["client_id"].startswith("alice-")
    assert body["topics"] == [f"users/{headers['user_id']}/devices/#"]
    assert body["expires_in_seconds"] == 86400


@pytest.mark.anyio
async def test_device_provisioning_flow(async_api_client, sample_device_payload) -> None:
    headers = await _register_and_login(async_api_client)
    auth_header = {"Authorization": headers["Authorization"]}

    create_resp = await async_api_client.post("/api/devices", headers=auth_header, json=sample_device_payload)
    assert create_resp.status_code == 201
    body = create_resp.json()
    assert body["device_id"] == sample_device_payload["device_id"]
    assert body["topics"] == [f"users/{headers['user_id']}/devices/lamp-1/#"]

    list_resp = await async_api_client.get("/api/devices", headers=auth_header)
    assert list_resp.status_code == 200
    devices = list_resp.json()["devices"]
    assert len(devices) == 1
    assert devices[0]["name"] == sample_device_payload["name"]

    delete_resp = await async_api_client.delete("/api/devices/lamp-1", headers=auth_header)
    assert delete_resp.status_code == 204

    list_resp_after = await async_api_client.get("/api/devices", headers=auth_header)
    assert list_resp_after.status_code == 200
    assert list_resp_after.json()["devices"] == []


@pytest.mark.anyio
async def test_duplicate_device_rejected(async_api_client) -> None:
    headers = await _register_and_login(async_api_client)
    auth_header = {"Authorization": headers["Authorization"]}
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = await async_api_client.post("/api/devices", headers=auth_header, json=payload)
    assert first.status_code == 201

    duplicate = await async_api_client.post("/api/devices", headers=auth_header, json=payload)
    assert duplicate.status_code == 400
    assert "already exists" in duplicate.json()["detail"]


@pytest.mark.anyio
@pytest.mark.parametrize("token", [None, "", "invalid.jwt.token"])
async def test_auth_required(async_api_client, token: str | None) -> None:
    headers = {"Authorization": f"Bearer {token}"} if token is not None else {}
    resp = await async_api_client.get("/api/devices", headers=headers)
    assert resp.status_code == 401
