from __future__ import annotations

from typing import List

from .models import (
    ProvisioningContext,
    ProvisioningRequest,
    ProvisioningResult,
    ProvisioningStage,
    ProvisioningStatus,
    ProvisioningStepResult,
)
from .registry import ProvisioningRegistry, registry


class ProvisioningService:
    def __init__(self, registry: ProvisioningRegistry | None = None) -> None:
        self.registry = registry or ProvisioningRegistry()

    async def provision(self, user_id: str, request: ProvisioningRequest) -> ProvisioningResult:
        strategy = self.registry.get_strategy(request)
        adapter = self.registry.get_adapter(request, strategy)
        context = ProvisioningContext(user_id=user_id, request=request)

        try:
            steps = await strategy.run(adapter, context)
            status = self._final_status(steps)
        except Exception as exc:  # pragma: no cover - defensive guard
            steps = [
                self._error_step(
                    detail=f"Unexpected failure: {exc}",
                    stage=ProvisioningStage.PROVISION,
                )
            ]
            status = ProvisioningStatus.FAILED

        if status == ProvisioningStatus.FAILED:
            rollback = await adapter.rollback(context)
            steps.append(rollback)

        return ProvisioningResult(
            device_id=request.device_id,
            strategy=strategy.key,
            adapter=adapter.name,
            status=status,
            steps=steps,
        )

    @staticmethod
    def _final_status(steps: List[ProvisioningStepResult]) -> ProvisioningStatus:
        if not steps:
            return ProvisioningStatus.FAILED
        if any(step.status == ProvisioningStatus.FAILED for step in steps):
            return ProvisioningStatus.FAILED
        return ProvisioningStatus.SUCCEEDED

    @staticmethod
    def _error_step(detail: str, stage: ProvisioningStage) -> ProvisioningStepResult:
        return ProvisioningStepResult(stage=stage, status=ProvisioningStatus.FAILED, detail=detail)


def get_provisioning_service() -> ProvisioningService:
    return ProvisioningService(registry)
