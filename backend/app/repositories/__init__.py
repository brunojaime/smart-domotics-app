"""Repository factories for storage backends."""

from functools import lru_cache
from pathlib import Path

from ..config import settings
from .base import RepositoryProvider, ZoneRepository
from .memory import create_in_memory_provider
from .sqlalchemy import create_sqlite_provider
from .zone_device_repository import ZoneDeviceRepository


@lru_cache
def get_repository_provider() -> RepositoryProvider:
    backend = settings.storage_backend.lower()
    if backend == "sqlite":
        db_path = Path(settings.sqlite_db_path)
        db_url = f"sqlite:///{db_path}"
        return create_sqlite_provider(db_url)
    if backend == "memory":
        return create_in_memory_provider()
    raise ValueError(f"Unknown storage backend: {backend}")


__all__ = [
    "RepositoryProvider",
    "ZoneRepository",
    "ZoneDeviceRepository",
    "get_repository_provider",
    "create_in_memory_provider",
    "create_sqlite_provider",
]
