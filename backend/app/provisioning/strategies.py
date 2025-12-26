from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Iterable

from .adapters import ProvisioningAdapter
from .models import ProvisioningContext, ProvisioningStage, ProvisioningStepResult, ProvisioningStatus


class ProvisioningStrategy(ABC):
    key: str
    required_capabilities: set[str]
    default_adapter: str

    def __init__(self, key: str, required_capabilities: Iterable[str], default_adapter: str) -> None:
        self.key = key
        self.required_capabilities = set(required_capabilities)
        self.default_adapter = default_adapter

    def supports(self, capabilities: Iterable[str]) -> bool:
        return self.required_capabilities.issubset(set(capabilities))

    @abstractmethod
    async def run(self, adapter: ProvisioningAdapter, context: ProvisioningContext) -> list[ProvisioningStepResult]:
        raise NotImplementedError


class WifiProvisioningStrategy(ProvisioningStrategy):
    def __init__(self) -> None:
        super().__init__(
            key="wifi",
            required_capabilities={"wifi"},
            default_adapter="wifi_local",
        )

    async def run(self, adapter: ProvisioningAdapter, context: ProvisioningContext) -> list[ProvisioningStepResult]:
        if not adapter.supports(self.required_capabilities):
            return [
                ProvisioningStepResult(
                    stage=ProvisioningStage.PRECHECK,
                    status=ProvisioningStatus.FAILED,
                    detail="Adapter missing wifi capability",
                )
            ]

        steps = [await adapter.connect(context)]
        if steps[-1].status == ProvisioningStatus.FAILED:
            return steps

        steps.append(
            ProvisioningStepResult(
                stage=ProvisioningStage.PRECHECK,
                status=ProvisioningStatus.SUCCEEDED,
                detail="Wi-Fi credentials validated",
            )
        )
        steps.append(await adapter.send_payload(context))
        steps.append(await adapter.finalize(context))
        return steps


class ZigbeeProvisioningStrategy(ProvisioningStrategy):
    def __init__(self) -> None:
        super().__init__(
            key="zigbee",
            required_capabilities={"zigbee"},
            default_adapter="zigbee_local",
        )

    async def run(self, adapter: ProvisioningAdapter, context: ProvisioningContext) -> list[ProvisioningStepResult]:
        steps = [
            ProvisioningStepResult(
                stage=ProvisioningStage.DETECT,
                status=ProvisioningStatus.SUCCEEDED,
                detail="Join request broadcasted",
            )
        ]
        if not adapter.supports(self.required_capabilities):
            steps.append(
                ProvisioningStepResult(
                    stage=ProvisioningStage.PRECHECK,
                    status=ProvisioningStatus.FAILED,
                    detail="Adapter missing zigbee capability",
                )
            )
            return steps

        steps.append(await adapter.send_payload(context))
        steps.append(await adapter.finalize(context))
        return steps
