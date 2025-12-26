import importlib
from typing import Dict

import pytest
from fastapi.testclient import TestClient

from backend.app.store import device_store


def _env_values() -> Dict[str, str]:
    return {
        "APP_TOKEN_PREFIX": "user_",
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

    import backend.app.config as config

    importlib.reload(config)
    yield
    importlib.reload(config)


@pytest.fixture(autouse=True)
def reset_device_store() -> None:
    device_store.clear()
    yield
    device_store.clear()


@pytest.fixture
def fastapi_app():
    import backend.app.main as main

    importlib.reload(main)
    return main.app


@pytest.fixture
def auth_header() -> Dict[str, str]:
    return {"Authorization": "Bearer user_alice"}


@pytest.fixture
def bearer_token() -> str:
    return "user_alice"


@pytest.fixture
def sample_device_payload() -> Dict[str, str]:
    return {"device_id": "lamp-1", "name": "Lamp"}


@pytest.fixture
def api_client(fastapi_app):
    with TestClient(fastapi_app) as client:
        yield client
