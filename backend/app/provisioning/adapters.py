from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Iterable, Set

from .models import ProvisioningContext, ProvisioningStage, ProvisioningStepResult, ProvisioningStatus


class ProvisioningAdapter(ABC):
    name: str
    capabilities: Set[str]

    def __init__(self, name: str, capabilities: Iterable[str]) -> None:
        self.name = name
        self.capabilities = set(capabilities)

    def supports(self, required: Iterable[str]) -> bool:
        return set(required).issubset(self.capabilities)

    @abstractmethod
    async def connect(self, context: ProvisioningContext) -> ProvisioningStepResult:
        raise NotImplementedError

    @abstractmethod
    async def send_payload(self, context: ProvisioningContext) -> ProvisioningStepResult:
        raise NotImplementedError

    @abstractmethod
    async def finalize(self, context: ProvisioningContext) -> ProvisioningStepResult:
        raise NotImplementedError

    @abstractmethod
    async def rollback(self, context: ProvisioningContext) -> ProvisioningStepResult:
        raise NotImplementedError


class InMemoryProvisioningAdapter(ProvisioningAdapter):
    def __init__(self, name: str, capabilities: Iterable[str]) -> None:
        super().__init__(name, capabilities)
        self.commands: list[str] = []

    async def connect(self, context: ProvisioningContext) -> ProvisioningStepResult:
        self.commands.append("connect")
        return ProvisioningStepResult(
            stage=ProvisioningStage.DETECT,
            status=ProvisioningStatus.SUCCEEDED,
            detail="Adapter connected",
        )

    async def send_payload(self, context: ProvisioningContext) -> ProvisioningStepResult:
        self.commands.append("send_payload")
        return ProvisioningStepResult(
            stage=ProvisioningStage.PROVISION,
            status=ProvisioningStatus.SUCCEEDED,
            detail="Payload delivered",
            metadata={"payload": context.request.payload},
        )

    async def finalize(self, context: ProvisioningContext) -> ProvisioningStepResult:
        self.commands.append("finalize")
        return ProvisioningStepResult(
            stage=ProvisioningStage.CONFIRM,
            status=ProvisioningStatus.SUCCEEDED,
            detail="Provisioning confirmed",
        )

    async def rollback(self, context: ProvisioningContext) -> ProvisioningStepResult:
        self.commands.append("rollback")
        return ProvisioningStepResult(
            stage=ProvisioningStage.ROLLBACK,
            status=ProvisioningStatus.SUCCEEDED,
            detail="Rollback completed",
        )
