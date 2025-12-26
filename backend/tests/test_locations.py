import pytest
from fastapi.testclient import TestClient


def test_location_building_zone_device_flow(api_client: TestClient) -> None:
    location_resp = api_client.post("/api/v1/locations", json={"name": "Home"})
    assert location_resp.status_code == 201
    location = location_resp.json()

    list_locations = api_client.get("/api/v1/locations")
    assert list_locations.status_code == 200
    assert len(list_locations.json()) == 1

    fetched_location = api_client.get(f"/api/v1/locations/{location['id']}")
    assert fetched_location.status_code == 200
    assert fetched_location.json()["name"] == "Home"

    updated_location = api_client.put(
        f"/api/v1/locations/{location['id']}", json={"name": "Primary Home"}
    )
    assert updated_location.status_code == 200
    assert updated_location.json()["name"] == "Primary Home"

    building_resp = api_client.post(
        f"/api/v1/locations/{location['id']}/buildings", json={"name": "Main"}
    )
    assert building_resp.status_code == 201
    building = building_resp.json()
    assert building["location_id"] == location["id"]

    buildings_list = api_client.get(f"/api/v1/locations/{location['id']}/buildings")
    assert buildings_list.status_code == 200
    assert len(buildings_list.json()) == 1

    fetched_building = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}"
    )
    assert fetched_building.status_code == 200
    assert fetched_building.json()["name"] == "Main"

    updated_building = api_client.put(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}",
        json={"name": "Annex"},
    )
    assert updated_building.status_code == 200
    assert updated_building.json()["name"] == "Annex"

    zone_resp = api_client.post(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones",
        json={"name": "Ground Floor"},
    )
    assert zone_resp.status_code == 201
    zone = zone_resp.json()
    assert zone["building_id"] == building["id"]

    zones_list = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones"
    )
    assert zones_list.status_code == 200
    assert len(zones_list.json()) == 1

    fetched_zone = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}"
    )
    assert fetched_zone.status_code == 200
    assert fetched_zone.json()["name"] == "Ground Floor"

    updated_zone = api_client.put(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}",
        json={"name": "Main Floor"},
    )
    assert updated_zone.status_code == 200
    assert updated_zone.json()["name"] == "Main Floor"

    device_resp = api_client.post(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}/devices",
        json={"device_id": "light-1", "name": "Hall Light"},
    )
    assert device_resp.status_code == 201
    device = device_resp.json()
    assert device["zone_id"] == zone["id"]

    devices_list = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}/devices"
    )
    assert devices_list.status_code == 200
    assert len(devices_list.json()) == 1

    fetched_device = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}/devices/{device['device_id']}"
    )
    assert fetched_device.status_code == 200
    assert fetched_device.json()["name"] == "Hall Light"

    delete_device = api_client.delete(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}/devices/{device['device_id']}"
    )
    assert delete_device.status_code == 204

    devices_after_delete = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}/devices"
    )
    assert devices_after_delete.status_code == 200
    assert devices_after_delete.json() == []

    delete_zone = api_client.delete(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/zones/{zone['id']}"
    )
    assert delete_zone.status_code == 204

    delete_building = api_client.delete(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}"
    )
    assert delete_building.status_code == 204

    buildings_after_delete = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings"
    )
    assert buildings_after_delete.status_code == 200
    assert buildings_after_delete.json() == []

    delete_location = api_client.delete(f"/api/v1/locations/{location['id']}")
    assert delete_location.status_code == 204

    missing_location = api_client.get(f"/api/v1/locations/{location['id']}")
    assert missing_location.status_code == 404


def test_parent_validation_errors(api_client: TestClient) -> None:
    building_resp = api_client.post(
        "/api/v1/locations/missing/buildings", json={"name": "Ghost"}
    )
    assert building_resp.status_code == 404

    location_resp = api_client.post("/api/v1/locations", json={"name": "Val"})
    location_id = location_resp.json()["id"]

    zone_resp = api_client.post(
        f"/api/v1/locations/{location_id}/buildings/missing/zones", json={"name": "Ghost"}
    )
    assert zone_resp.status_code == 404


def test_update_requires_fields(api_client: TestClient) -> None:
    location_resp = api_client.post("/api/v1/locations", json={"name": "Partial"})
    location_id = location_resp.json()["id"]

    empty_update = api_client.put(f"/api/v1/locations/{location_id}", json={})
    assert empty_update.status_code == 400
