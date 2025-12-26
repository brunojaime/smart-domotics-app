from fastapi import APIRouter, HTTPException, status

from ..container import area_service
from ..dto.structures import AreaCreateRequest, AreaResponse, AreaUpdateRequest

router = APIRouter(
    prefix="/locations/{location_id}/buildings/{building_id}/zones/{zone_id}/areas",
    tags=["areas"],
)


@router.get("", response_model=list[AreaResponse])
async def list_areas(
    location_id: str,
    building_id: str,
    zone_id: str,
) -> list[AreaResponse]:
    try:
        return area_service.list_areas(location_id, building_id, zone_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=AreaResponse, status_code=status.HTTP_201_CREATED)
async def create_area(
    location_id: str,
    building_id: str,
    zone_id: str,
    payload: AreaCreateRequest,
) -> AreaResponse:
    try:
        return area_service.create_area(location_id, building_id, zone_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/{area_id}", response_model=AreaResponse)
async def get_area(
    location_id: str,
    building_id: str,
    zone_id: str,
    area_id: str,
) -> AreaResponse:
    try:
        return area_service.get_area(location_id, building_id, zone_id, area_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.put("/{area_id}", response_model=AreaResponse)
async def update_area(
    location_id: str,
    building_id: str,
    zone_id: str,
    area_id: str,
    payload: AreaUpdateRequest,
) -> AreaResponse:
    try:
        return area_service.update_area(location_id, building_id, zone_id, area_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/{area_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_area(
    location_id: str,
    building_id: str,
    zone_id: str,
    area_id: str,
) -> None:
    try:
        area_service.delete_area(location_id, building_id, zone_id, area_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
