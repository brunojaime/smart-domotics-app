import pytest
from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials

from backend.app.main import get_current_user
from backend.app.store import device_store


@pytest.mark.anyio
async def test_create_list_delete_device_flow(sample_device_payload: dict) -> None:
    device = device_store.create_device("alice", sample_device_payload["device_id"], sample_device_payload["name"])
    assert device.id == sample_device_payload["device_id"]

    devices = device_store.list_devices("alice")
    assert len(devices) == 1
    assert devices[0].name == sample_device_payload["name"]

    device_store.delete_device("alice", sample_device_payload["device_id"])
    assert device_store.list_devices("alice") == []


@pytest.mark.anyio
async def test_duplicate_device_rejected(sample_device_payload: dict) -> None:
    device_store.create_device("alice", sample_device_payload["device_id"], sample_device_payload["name"])

    with pytest.raises(ValueError):
        device_store.create_device("alice", sample_device_payload["device_id"], sample_device_payload["name"])


@pytest.mark.anyio
async def test_delete_missing_device_raises() -> None:
    with pytest.raises(KeyError):
        device_store.delete_device("alice", "missing")


@pytest.mark.anyio
async def test_get_current_user_accepts_valid_token() -> None:
    credentials = HTTPAuthorizationCredentials(scheme="Bearer", credentials="user_bob")
    user = await get_current_user(credentials)

    assert user.id == "bob"


@pytest.mark.anyio
async def test_get_current_user_rejects_invalid_tokens() -> None:
    invalid_prefix = HTTPAuthorizationCredentials(scheme="Bearer", credentials="invalid_bob")
    empty_id = HTTPAuthorizationCredentials(scheme="Bearer", credentials="user_")

    with pytest.raises(HTTPException):
        await get_current_user(invalid_prefix)

    with pytest.raises(HTTPException):
        await get_current_user(empty_id)
