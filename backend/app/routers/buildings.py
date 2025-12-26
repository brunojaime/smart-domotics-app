from fastapi import APIRouter, HTTPException, status

from ..container import building_service, room_service, zone_device_service, zone_service
from ..models import BuildingCreate, BuildingResponse, BuildingUpdate

router = APIRouter(prefix="/locations/{location_id}/buildings", tags=["buildings"])


@router.get("", response_model=list[BuildingResponse])
async def list_buildings(location_id: str) -> list[BuildingResponse]:
    try:
        return building_service.list_buildings(location_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=BuildingResponse, status_code=status.HTTP_201_CREATED)
async def create_building(location_id: str, payload: BuildingCreate) -> BuildingResponse:
    try:
        return building_service.create_building(location_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/{building_id}", response_model=BuildingResponse)
async def get_building(location_id: str, building_id: str) -> BuildingResponse:
    try:
        return building_service.get_building(location_id, building_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.put("/{building_id}", response_model=BuildingResponse)
async def update_building(location_id: str, building_id: str, payload: BuildingUpdate) -> BuildingResponse:
    try:
        return building_service.update_building(location_id, building_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/{building_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_building(location_id: str, building_id: str) -> None:
    try:
        for zone in zone_service.list_zones(location_id, building_id):
            zone_device_service.delete_devices_for_zone(zone.id)
        zone_service.delete_zones_for_building(building_id)
        building_service.delete_building(location_id, building_id)
        room_service.delete_rooms_for_building(building_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
