import pytest
from fastapi.testclient import TestClient


def test_location_building_room_flow(api_client: TestClient) -> None:
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

    room_resp = api_client.post(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/rooms",
        json={"name": "Living Room"},
    )
    assert room_resp.status_code == 201
    room = room_resp.json()
    assert room["building_id"] == building["id"]

    rooms_list = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/rooms"
    )
    assert rooms_list.status_code == 200
    assert len(rooms_list.json()) == 1

    fetched_room = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/rooms/{room['id']}"
    )
    assert fetched_room.status_code == 200
    assert fetched_room.json()["name"] == "Living Room"

    updated_room = api_client.put(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/rooms/{room['id']}",
        json={"name": "Family Room"},
    )
    assert updated_room.status_code == 200
    assert updated_room.json()["name"] == "Family Room"

    delete_room = api_client.delete(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/rooms/{room['id']}"
    )
    assert delete_room.status_code == 204

    rooms_after_delete = api_client.get(
        f"/api/v1/locations/{location['id']}/buildings/{building['id']}/rooms"
    )
    assert rooms_after_delete.status_code == 200
    assert rooms_after_delete.json() == []

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

    room_resp = api_client.post(
        f"/api/v1/locations/{location_id}/buildings/missing/rooms", json={"name": "Ghost"}
    )
    assert room_resp.status_code == 404


def test_update_requires_fields(api_client: TestClient) -> None:
    location_resp = api_client.post("/api/v1/locations", json={"name": "Partial"})
    location_id = location_resp.json()["id"]

    empty_update = api_client.put(f"/api/v1/locations/{location_id}", json={})
    assert empty_update.status_code == 400
