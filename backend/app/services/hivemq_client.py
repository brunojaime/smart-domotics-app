from typing import List

from ..config import settings


def user_topics(user_id: str) -> List[str]:
    return [f"users/{user_id}/devices/#"]


def device_topics(user_id: str, device_id: str) -> List[str]:
    return [f"users/{user_id}/devices/{device_id}/#"]


def build_mqtt_credentials(user_id: str, client_suffix: str = "app") -> dict:
    client_id = f"{user_id}-{client_suffix}"
    return {
        "host": settings.hivemq_host,
        "port": settings.hivemq_port,
        "username": settings.hivemq_username,
        "password": settings.hivemq_password,
        "client_id": client_id,
        "topics": user_topics(user_id),
        "expires_in_seconds": settings.mqtt_credentials_ttl,
    }
