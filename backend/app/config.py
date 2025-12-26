from .settings import AppSettings as Settings, settings

__all__ = ["Settings", "settings"]
from pydantic import BaseSettings, Field, validator


class Settings(BaseSettings):
    app_token_prefix: str = Field("user_", env="APP_TOKEN_PREFIX")
    user_repository_backend: str = Field("memory", env="USER_REPOSITORY")
    jwt_secret: str = Field("dev_super_secret_change_later", env="APP_JWT_SECRET")
    jwt_algorithm: str = Field("HS256", env="APP_JWT_ALGORITHM")
    access_token_expire_minutes: int = Field(30, env="APP_ACCESS_TOKEN_EXPIRE_MINUTES")
    refresh_token_expire_minutes: int = Field(60 * 24 * 14, env="APP_REFRESH_TOKEN_EXPIRE_MINUTES")
    google_client_id: str = Field("", env="GOOGLE_CLIENT_ID")
    google_client_secret: str = Field("", env="GOOGLE_CLIENT_SECRET")
    google_redirect_uri: str = Field("http://localhost:8000/api/auth/google/callback", env="GOOGLE_REDIRECT_URI")
    hivemq_host: str = Field("localhost", env="HIVEMQ_HOST")
    hivemq_port: int = Field(1883, env="HIVEMQ_PORT")
    hivemq_username: str = Field("local_backend", env="HIVEMQ_USERNAME")
    hivemq_password: str = Field("local_backend_password", env="HIVEMQ_PASSWORD")
    mqtt_credentials_ttl: int = Field(86400, env="MQTT_CREDENTIALS_TTL")
    storage_backend: str = Field("memory", env="STORAGE_BACKEND")
    sqlite_db_path: str = Field("./data/domotics.sqlite", env="SQLITE_DB_PATH")
    cors_allowed_origins: List[str] = Field(
        ["http://localhost:3000", "http://localhost:5173"], env="CORS_ALLOWED_ORIGINS"
    )

    @validator("cors_allowed_origins", pre=True, allow_reuse=True)
    def split_origins(cls, value: str | List[str]) -> List[str]:
        if isinstance(value, str):
            return [origin.strip() for origin in value.split(",") if origin.strip()]
        return value

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

        @classmethod
        def parse_env_var(cls, field_name: str, raw_val: str):
            if field_name == "cors_allowed_origins":
                if raw_val.strip().startswith("["):
                    return json.loads(raw_val)
                return [origin.strip() for origin in raw_val.split(",") if origin.strip()]
            return cls.json_loads(raw_val)


settings = Settings()
