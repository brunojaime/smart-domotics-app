import pytest


def test_health(api_client) -> None:
    response = api_client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_issue_mqtt_credentials(api_client, auth_header) -> None:
    response = api_client.post("/api/auth/mqtt", headers=auth_header)
    assert response.status_code == 200
    body = response.json()
    assert body["host"] == "localhost"
    assert body["username"] == "local_backend"
    assert body["client_id"].startswith("alice-")
    assert body["topics"] == ["users/alice/devices/#"]
    assert body["expires_in_seconds"] == 86400


def test_device_provisioning_flow(api_client, auth_header, sample_device_payload) -> None:
    create_resp = api_client.post("/api/devices", headers=auth_header, json=sample_device_payload)
    assert create_resp.status_code == 201
    body = create_resp.json()
    assert body["device_id"] == sample_device_payload["device_id"]
    assert body["topics"] == ["users/alice/devices/lamp-1/#"]

    list_resp = api_client.get("/api/devices", headers=auth_header)
    assert list_resp.status_code == 200
    devices = list_resp.json()["devices"]
    assert len(devices) == 1
    assert devices[0]["name"] == sample_device_payload["name"]

    delete_resp = api_client.delete("/api/devices/lamp-1", headers=auth_header)
    assert delete_resp.status_code == 204

    list_resp_after = api_client.get("/api/devices", headers=auth_header)
    assert list_resp_after.status_code == 200
    assert list_resp_after.json()["devices"] == []


def test_duplicate_device_rejected(api_client, auth_header) -> None:
    payload = {"device_id": "dup-1", "name": "Dup"}
    first = api_client.post("/api/devices", headers=auth_header, json=payload)
    assert first.status_code == 201

    duplicate = api_client.post("/api/devices", headers=auth_header, json=payload)
    assert duplicate.status_code == 400
    assert "already exists" in duplicate.json()["detail"]


@pytest.mark.parametrize("token", [None, "", "otherprefix_user"])
def test_auth_required(api_client, token: str | None) -> None:
    headers = {"Authorization": f"Bearer {token}"} if token is not None else {}
    resp = api_client.get("/api/devices", headers=headers)
    assert resp.status_code == 401
