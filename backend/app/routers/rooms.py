from fastapi import APIRouter, HTTPException, status

from ..container import room_service
from ..models import RoomCreate, RoomResponse, RoomUpdate

router = APIRouter(
    prefix="/locations/{location_id}/buildings/{building_id}/rooms",
    tags=["rooms"],
)


@router.get("", response_model=list[RoomResponse])
async def list_rooms(location_id: str, building_id: str) -> list[RoomResponse]:
    try:
        return room_service.list_rooms(location_id, building_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.post("", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
async def create_room(location_id: str, building_id: str, payload: RoomCreate) -> RoomResponse:
    try:
        return room_service.create_room(location_id, building_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.get("/{room_id}", response_model=RoomResponse)
async def get_room(location_id: str, building_id: str, room_id: str) -> RoomResponse:
    try:
        return room_service.get_room(location_id, building_id, room_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc


@router.put("/{room_id}", response_model=RoomResponse)
async def update_room(location_id: str, building_id: str, room_id: str, payload: RoomUpdate) -> RoomResponse:
    try:
        return room_service.update_room(location_id, building_id, room_id, payload)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc


@router.delete("/{room_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_room(location_id: str, building_id: str, room_id: str) -> None:
    try:
        room_service.delete_room(location_id, building_id, room_id)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
