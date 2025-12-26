import importlib
import os
from typing import Generator

import pytest
from fastapi.testclient import TestClient

from backend.app.container import reset_repositories
from backend.app.store import device_store


def override_env() -> None:
    os.environ.update(
        {
            "APP_TOKEN_PREFIX": "user_",
            "HIVEMQ_HOST": "localhost",
            "HIVEMQ_PORT": "1883",
            "HIVEMQ_USERNAME": "local_backend",
            "HIVEMQ_PASSWORD": "local_backend_password",
            "MQTT_CREDENTIALS_TTL": "86400",
            "CORS_ALLOWED_ORIGINS": "http://localhost:3000,http://localhost:5173",
        }
    )


def client() -> TestClient:
    override_env()
    import backend.app.config as config

    importlib.reload(config)
    import backend.app.main as main

    importlib.reload(main)
    return TestClient(main.app)


@pytest.fixture
def api_client() -> Generator[TestClient, None, None]:
    c = client()
    try:
        yield c
    finally:
        device_store.clear()
        reset_repositories()
