from __future__ import annotations

from ..models import Device, ZoneDeviceCreate
from ..repositories import ZoneDeviceRepository, ZoneRepository


class ZoneDeviceService:
    def __init__(self, zone_repository: ZoneRepository, device_repository: ZoneDeviceRepository) -> None:
        self._zone_repository = zone_repository
        self._device_repository = device_repository

    def list_devices(self, location_id: str, building_id: str, zone_id: str) -> list[Device]:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        return self._device_repository.list_by_zone(zone_id)

    def create_device(self, location_id: str, building_id: str, zone_id: str, data: ZoneDeviceCreate) -> Device:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        device = Device(id=data.device_id, name=data.name, zone_id=zone_id)
        return self._device_repository.add(device)

    def get_device(self, location_id: str, building_id: str, zone_id: str, device_id: str) -> Device:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        device = self._device_repository.get(zone_id, device_id)
        if device is None:
            raise KeyError("Device not found")
        return device

    def delete_device(self, location_id: str, building_id: str, zone_id: str, device_id: str) -> None:
        self._ensure_zone_exists(location_id, building_id, zone_id)
        self._device_repository.delete(zone_id, device_id)

    def delete_devices_for_zone(self, zone_id: str) -> None:
        self._device_repository.delete_by_zone(zone_id)

    def _ensure_zone_exists(self, location_id: str, building_id: str, zone_id: str) -> None:
        zone = self._zone_repository.get(zone_id)
        if zone is None or zone.building_id != building_id:
            raise KeyError("Zone not found")
