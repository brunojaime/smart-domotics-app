from __future__ import annotations

from typing import Dict

from .adapters import InMemoryProvisioningAdapter, ProvisioningAdapter
from .models import ProvisioningRequest
from .strategies import ProvisioningStrategy, WifiProvisioningStrategy, ZigbeeProvisioningStrategy


class ProvisioningRegistry:
    def __init__(self) -> None:
        self._strategies: Dict[str, ProvisioningStrategy] = {}
        self._adapters: Dict[str, ProvisioningAdapter] = {}
        self._bootstrap_defaults()

    def _bootstrap_defaults(self) -> None:
        self.register_strategy(WifiProvisioningStrategy())
        self.register_strategy(ZigbeeProvisioningStrategy())
        self.register_adapter("wifi_local", InMemoryProvisioningAdapter("wifi_local", {"wifi"}))
        self.register_adapter("zigbee_local", InMemoryProvisioningAdapter("zigbee_local", {"zigbee"}))

    def register_strategy(self, strategy: ProvisioningStrategy) -> None:
        self._strategies[strategy.key] = strategy

    def register_adapter(self, name: str, adapter: ProvisioningAdapter) -> None:
        self._adapters[name] = adapter

    def get_strategy(self, request: ProvisioningRequest) -> ProvisioningStrategy:
        key = request.strategy or request.device_type
        if key not in self._strategies:
            raise KeyError(f"Unsupported strategy '{key}'")
        return self._strategies[key]

    def get_adapter(self, request: ProvisioningRequest, strategy: ProvisioningStrategy) -> ProvisioningAdapter:
        name = request.adapter or strategy.default_adapter
        if name not in self._adapters:
            raise KeyError(f"Unsupported adapter '{name}'")
        return self._adapters[name]

    def reset(self) -> None:
        self._strategies.clear()
        self._adapters.clear()
        self._bootstrap_defaults()


registry = ProvisioningRegistry()
