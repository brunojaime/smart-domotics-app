from fastapi import APIRouter, HTTPException, status

from ..container import building_service
from ..dto.structures import BuildingCreateRequest, BuildingResponse, BuildingUpdateRequest

router = APIRouter(prefix="/locations/{location_id}/buildings", tags=["buildings"])


@router.get("", response_model=list[BuildingResponse])
async def list_buildings(location_id: str) -> list[BuildingResponse]:
    try:
        return building_service.list_buildings(location_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=BuildingResponse, status_code=status.HTTP_201_CREATED)
async def create_building(location_id: str, payload: BuildingCreateRequest) -> BuildingResponse:
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
async def update_building(location_id: str, building_id: str, payload: BuildingUpdateRequest) -> BuildingResponse:
    try:
        return building_service.update_building(location_id, building_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/{building_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_building(location_id: str, building_id: str) -> None:
    try:
        building_service.delete_building(location_id, building_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
