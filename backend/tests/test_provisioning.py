import pytest

from backend.app.provisioning.models import ProvisioningRequest, ProvisioningStatus
from backend.app.provisioning.registry import ProvisioningRegistry
from backend.app.provisioning.service import ProvisioningService


@pytest.mark.anyio
async def test_registry_selects_strategy_from_device_type() -> None:
    registry = ProvisioningRegistry()
    service = ProvisioningService(registry)

    request = ProvisioningRequest(
        device_id="device-1",
        device_type="wifi",
        capabilities=["wifi"],
        payload={"ssid": "Home", "password": "secret"},
    )

    result = await service.provision("alice", request)

    assert result.status == ProvisioningStatus.SUCCEEDED
    assert [step.stage.value for step in result.steps] == ["detect", "precheck", "provision", "confirm"]


@pytest.mark.anyio
async def test_failed_precheck_triggers_rollback() -> None:
    registry = ProvisioningRegistry()
    service = ProvisioningService(registry)

    request = ProvisioningRequest(
        device_id="device-2",
        device_type="zigbee",
        strategy="zigbee",
        adapter="wifi_local",  # intentionally missing zigbee capability
        capabilities=["zigbee"],
        payload={"channel": 15},
    )

    result = await service.provision("alice", request)

    assert result.status == ProvisioningStatus.FAILED
    assert result.steps[-1].stage.value == "rollback"


@pytest.mark.anyio
async def test_integration_endpoint_provisions_device(async_api_client) -> None:
    registration = await async_api_client.post(
        "/api/auth/register",
        json={"username": "bob", "password": "secret123", "email": "bob@example.com"},
    )
    tokens = registration.json()

    payload = {
        "device_id": "sensor-1",
        "device_type": "wifi",
        "capabilities": ["wifi"],
        "payload": {"ssid": "Lab", "password": "labpass"},
    }

    response = await async_api_client.post(
        "/api/v1/provisioning/devices/sensor-1",
        headers={"Authorization": f"Bearer {tokens['access_token']}"},
        json=payload,
    )

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == ProvisioningStatus.SUCCEEDED.value
    assert body["strategy"] == "wifi"
    assert [step["stage"] for step in body["steps"]] == ["detect", "precheck", "provision", "confirm"]
