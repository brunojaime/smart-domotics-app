from fastapi import APIRouter, HTTPException, status

from ..container import location_service
from ..models import LocationCreate, LocationResponse, LocationUpdate

router = APIRouter(prefix="/locations", tags=["locations"])


@router.get("", response_model=list[LocationResponse])
async def list_locations() -> list[LocationResponse]:
    return location_service.list_locations()


@router.post("", response_model=LocationResponse, status_code=status.HTTP_201_CREATED)
async def create_location(payload: LocationCreate) -> LocationResponse:
    try:
        return location_service.create_location(payload)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/{location_id}", response_model=LocationResponse)
async def get_location(location_id: str) -> LocationResponse:
    try:
        return location_service.get_location(location_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.put("/{location_id}", response_model=LocationResponse)
async def update_location(location_id: str, payload: LocationUpdate) -> LocationResponse:
    try:
        return location_service.update_location(location_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/{location_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_location(location_id: str) -> None:
    try:
        location_service.delete_location(location_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
