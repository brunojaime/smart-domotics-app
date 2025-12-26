import importlib
from typing import AsyncGenerator, Dict, Generator

import httpx
import pytest
from fastapi.testclient import TestClient
from httpx import ASGITransport

from backend.app.container import reset_repositories
from backend.app.store import device_store


def _env_values() -> Dict[str, str]:
    return {
        "APP_TOKEN_PREFIX": "user_",
        "APP_JWT_SECRET": "test_jwt_secret",
        "APP_JWT_ALGORITHM": "HS256",
        "APP_ACCESS_TOKEN_EXPIRE_MINUTES": "30",
        "APP_REFRESH_TOKEN_EXPIRE_MINUTES": "60",
        "APP_DATABASE_BACKEND": "memory",
        "APP_OAUTH_CLIENT_IDS": "{}",
        "USER_REPOSITORY": "memory",
        "HIVEMQ_HOST": "localhost",
        "HIVEMQ_PORT": "1883",
        "HIVEMQ_USERNAME": "local_backend",
        "HIVEMQ_PASSWORD": "local_backend_password",
        "MQTT_CREDENTIALS_TTL": "86400",
        "CORS_ALLOWED_ORIGINS": "http://localhost:3000,http://localhost:5173",
    }


@pytest.fixture(autouse=True)
def set_env(monkeypatch: pytest.MonkeyPatch) -> None:
    for key, value in _env_values().items():
        monkeypatch.setenv(key, value)
    yield


def _reload_app():
    import backend.app.settings as settings_module
    import backend.app.config as config_module
    import backend.app.auth as auth_module
    import backend.app.main as main_module

    importlib.reload(settings_module)
    importlib.reload(config_module)
    importlib.reload(auth_module)
    importlib.reload(main_module)
    return main_module.app


@pytest.fixture
def fastapi_app():
    return _reload_app()


@pytest.fixture
def api_client(fastapi_app) -> Generator[TestClient, None, None]:
    with TestClient(fastapi_app) as client:
        yield client


@pytest.fixture
async def async_api_client(fastapi_app) -> AsyncGenerator[httpx.AsyncClient, None]:
    from backend.app.main import build_device_repository

    if not hasattr(fastapi_app.state, "device_repository"):
        fastapi_app.state.device_repository = build_device_repository()
    await fastapi_app.router.startup()
    transport = ASGITransport(app=fastapi_app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        yield client
    await fastapi_app.router.shutdown()


@pytest.fixture(autouse=True)
def reset_state(fastapi_app) -> None:
    yield
    device_store.clear()
    reset_repositories()
    repository = getattr(fastapi_app.state, "device_repository", None)
    if repository and hasattr(repository, "clear"):
        repository.clear()


@pytest.fixture
def auth_header() -> Dict[str, str]:
    return {"Authorization": "Bearer user_alice"}


@pytest.fixture
def bearer_token() -> str:
    return "user_alice"


@pytest.fixture
def sample_device_payload() -> Dict[str, str]:
    return {"device_id": "lamp-1", "name": "Lamp"}
