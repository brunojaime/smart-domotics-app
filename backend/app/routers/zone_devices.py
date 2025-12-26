from fastapi import APIRouter, HTTPException, status

from ..container import zone_device_service
from ..models import ZoneDeviceCreate, ZoneDeviceResponse

router = APIRouter(
    prefix="/locations/{location_id}/buildings/{building_id}/zones/{zone_id}/devices",
    tags=["zone-devices"],
)


@router.get("", response_model=list[ZoneDeviceResponse])
async def list_devices(location_id: str, building_id: str, zone_id: str) -> list[ZoneDeviceResponse]:
    try:
        devices = zone_device_service.list_devices(location_id, building_id, zone_id)
        return [ZoneDeviceResponse(device_id=d.id, name=d.name, zone_id=d.zone_id or "") for d in devices]
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=ZoneDeviceResponse, status_code=status.HTTP_201_CREATED)
async def create_device(
    location_id: str, building_id: str, zone_id: str, payload: ZoneDeviceCreate
) -> ZoneDeviceResponse:
    try:
        device = zone_device_service.create_device(location_id, building_id, zone_id, payload)
        return ZoneDeviceResponse(device_id=device.id, name=device.name, zone_id=device.zone_id or "")
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/{device_id}", response_model=ZoneDeviceResponse)
async def get_device(location_id: str, building_id: str, zone_id: str, device_id: str) -> ZoneDeviceResponse:
    try:
        device = zone_device_service.get_device(location_id, building_id, zone_id, device_id)
        return ZoneDeviceResponse(device_id=device.id, name=device.name, zone_id=device.zone_id or "")
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.delete("/{device_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_device(location_id: str, building_id: str, zone_id: str, device_id: str) -> None:
    try:
        zone_device_service.delete_device(location_id, building_id, zone_id, device_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
