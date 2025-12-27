from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from ..models import User
from ..provisioning.models import ProvisioningRequest, ProvisioningResult
from ..provisioning.service import ProvisioningService, get_provisioning_service

router = APIRouter(prefix="/provisioning", tags=["provisioning"])
security = HTTPBearer(auto_error=False)


async def _get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security),
) -> User:
    from ..main import get_current_user

    return await get_current_user(credentials)


def _get_service() -> ProvisioningService:
    return get_provisioning_service()


@router.post("/devices/{device_id}", response_model=ProvisioningResult)
async def provision_device(
    device_id: str,
    request: ProvisioningRequest,
    current_user: User = Depends(_get_current_user),
    service: ProvisioningService = Depends(_get_service),
) -> ProvisioningResult:
    if device_id != request.device_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Device id mismatch")
    try:
        result = await service.provision(current_user.id, request)
    except KeyError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return result
