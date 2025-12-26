from __future__ import annotations

from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, validator


class ProvisioningStage(str, Enum):
    DETECT = "detect"
    PRECHECK = "precheck"
    PROVISION = "provision"
    CONFIRM = "confirm"
    ROLLBACK = "rollback"


class ProvisioningStatus(str, Enum):
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    SUCCEEDED = "succeeded"
    FAILED = "failed"


class ProvisioningStepResult(BaseModel):
    stage: ProvisioningStage
    status: ProvisioningStatus
    detail: str | None = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class ProvisioningRequest(BaseModel):
    device_id: str = Field(..., min_length=1)
    strategy: str | None = Field(default=None, description="Strategy key to use; falls back to device type if missing")
    adapter: str | None = Field(default=None, description="Adapter key to use; defaults to the strategy's preferred adapter")
    device_type: str = Field(..., min_length=1, description="Logical device type (e.g., wifi_sensor, zigbee_light)")
    capabilities: List[str] = Field(default_factory=list)
    payload: Dict[str, Any] = Field(default_factory=dict)

    @validator("capabilities", pre=True)
    def default_capabilities(cls, value: Optional[List[str]]) -> List[str]:
        if value is None:
            return []
        return value


class ProvisioningResult(BaseModel):
    device_id: str
    strategy: str
    adapter: str
    status: ProvisioningStatus
    steps: List[ProvisioningStepResult]

    @property
    def succeeded(self) -> bool:  # pragma: no cover - simple passthrough
        return self.status == ProvisioningStatus.SUCCEEDED


class ProvisioningContext(BaseModel):
    user_id: str
    request: ProvisioningRequest

    class Config:
        arbitrary_types_allowed = True
