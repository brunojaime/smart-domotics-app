import pytest
from fastapi.testclient import TestClient


AUTH_HEADER = {"Authorization": "Bearer user_alice"}


def test_health(api_client: TestClient) -> None:
    response = api_client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_issue_mqtt_credentials(api_client: TestClient) -> None:
    response = api_client.post("/api/auth/mqtt", headers=AUTH_HEADER)
    assert response.status_code == 200
    body = response.json()
    assert body["host"] == "localhost"
    assert body["username"] == "local_backend"
    assert body["client_id"].startswith("alice-")
    assert body["topics"] == ["users/alice/devices/#"]
    assert body["expires_in_seconds"] == 86400


def test_device_provisioning_flow(api_client: TestClient) -> None:
    create_resp = api_client.post(
        "/api/devices", headers=AUTH_HEADER, json={"device_id": "lamp-1", "name": "Lamp"}
    )
    assert create_resp.status_code == 201
    body = create_resp.json()
    assert body["device_id"] == "lamp-1"
    assert body["topics"] == ["users/alice/devices/lamp-1/#"]

    list_resp = api_client.get("/api/devices", headers=AUTH_HEADER)
    assert list_resp.status_code == 200
    devices = list_resp.json()["devices"]
    assert len(devices) == 1
    assert devices[0]["name"] == "Lamp"

    delete_resp = api_client.delete("/api/devices/lamp-1", headers=AUTH_HEADER)
    assert delete_resp.status_code == 204

    list_resp_after = api_client.get("/api/devices", headers=AUTH_HEADER)
    assert list_resp_after.status_code == 200
    assert list_resp_after.json()["devices"] == []


def test_duplicate_device_rejected(api_client: TestClient) -> None:
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = api_client.post("/api/devices", headers=AUTH_HEADER, json=payload)
    assert first.status_code == 201

    duplicate = api_client.post("/api/devices", headers=AUTH_HEADER, json=payload)
    assert duplicate.status_code == 400
    assert "already exists" in duplicate.json()["detail"]


@pytest.mark.parametrize("token", [None, "", "otherprefix_user"])
def test_auth_required(api_client: TestClient, token: str | None) -> None:
    headers = {"Authorization": f"Bearer {token}"} if token is not None else {}
    resp = api_client.get("/api/devices", headers=headers)
    assert resp.status_code == 401
