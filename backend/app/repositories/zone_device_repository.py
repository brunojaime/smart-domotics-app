from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Dict, List

from ..models import Device


class ZoneDeviceRepository(ABC):
    @abstractmethod
    def list_by_zone(self, zone_id: str) -> List[Device]:
        raise NotImplementedError

    @abstractmethod
    def add(self, device: Device) -> Device:
        raise NotImplementedError

    @abstractmethod
    def get(self, zone_id: str, device_id: str) -> Device | None:
        raise NotImplementedError

    @abstractmethod
    def delete(self, zone_id: str, device_id: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def delete_by_zone(self, zone_id: str) -> None:
        raise NotImplementedError


class InMemoryZoneDeviceRepository(ZoneDeviceRepository):
    def __init__(self) -> None:
        self._devices: Dict[str, Dict[str, Device]] = {}

    def list_by_zone(self, zone_id: str) -> List[Device]:
        return list(self._devices.get(zone_id, {}).values())

    def add(self, device: Device) -> Device:
        zone_devices = self._devices.setdefault(device.zone_id or "", {})
        if device.id in zone_devices:
            raise ValueError("Device already exists")
        zone_devices[device.id] = device
        return device

    def get(self, zone_id: str, device_id: str) -> Device | None:
        return self._devices.get(zone_id, {}).get(device_id)

    def delete(self, zone_id: str, device_id: str) -> None:
        zone_devices = self._devices.get(zone_id, {})
        if device_id not in zone_devices:
            raise KeyError("Device not found")
        del zone_devices[device_id]

    def delete_by_zone(self, zone_id: str) -> None:
        self._devices.pop(zone_id, None)


__all__ = ["ZoneDeviceRepository", "InMemoryZoneDeviceRepository"]
