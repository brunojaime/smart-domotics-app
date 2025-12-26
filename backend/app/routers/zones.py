from fastapi import APIRouter, HTTPException, status

from ..container import zone_service
from ..models import ZoneCreate, ZoneResponse, ZoneUpdate

router = APIRouter(
    prefix="/locations/{location_id}/buildings/{building_id}/zones",
    tags=["zones"],
)


@router.get("", response_model=list[ZoneResponse])
async def list_zones(location_id: str, building_id: str) -> list[ZoneResponse]:
    try:
        return zone_service.list_zones(location_id, building_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=ZoneResponse, status_code=status.HTTP_201_CREATED)
async def create_zone(location_id: str, building_id: str, payload: ZoneCreate) -> ZoneResponse:
    try:
        return zone_service.create_zone(location_id, building_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/{zone_id}", response_model=ZoneResponse)
async def get_zone(location_id: str, building_id: str, zone_id: str) -> ZoneResponse:
    try:
        return zone_service.get_zone(location_id, building_id, zone_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.put("/{zone_id}", response_model=ZoneResponse)
async def update_zone(location_id: str, building_id: str, zone_id: str, payload: ZoneUpdate) -> ZoneResponse:
    try:
        return zone_service.update_zone(location_id, building_id, zone_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/{zone_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_zone(location_id: str, building_id: str, zone_id: str) -> None:
    try:
        zone_service.delete_zone(location_id, building_id, zone_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
