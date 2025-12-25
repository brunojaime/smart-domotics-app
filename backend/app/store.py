from typing import Dict, List

from .models import Device


class DeviceStore:
    def __init__(self) -> None:
        self._devices: Dict[str, Dict[str, Device]] = {}

    def create_device(self, owner_id: str, device_id: str, name: str) -> Device:
        owner_devices = self._devices.setdefault(owner_id, {})
        if device_id in owner_devices:
            raise ValueError("Device already exists")
        device = Device(id=device_id, name=name, owner_id=owner_id)
        owner_devices[device_id] = device
        return device

    def list_devices(self, owner_id: str) -> List[Device]:
        return list(self._devices.get(owner_id, {}).values())

    def delete_device(self, owner_id: str, device_id: str) -> None:
        owner_devices = self._devices.get(owner_id, {})
        if device_id not in owner_devices:
            raise KeyError("Device not found")
        del owner_devices[device_id]

    def clear(self) -> None:
        self._devices.clear()


device_store = DeviceStore()
