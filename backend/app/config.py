from typing import List
import json

from pydantic import BaseSettings, Field, validator


class Settings(BaseSettings):
    app_token_prefix: str = Field("user-", env="APP_TOKEN_PREFIX")
    jwt_secret: str = Field("changeme", env="APP_JWT_SECRET")
    hivemq_host: str = Field("your-hivemq-host", env="HIVEMQ_HOST")
    hivemq_port: int = Field(8883, env="HIVEMQ_PORT")
    hivemq_username: str = Field("hivemq-user", env="HIVEMQ_USERNAME")
    hivemq_password: str = Field("hivemq-pass", env="HIVEMQ_PASSWORD")
    mqtt_credentials_ttl: int = Field(3600, env="MQTT_CREDENTIALS_TTL")
    cors_allowed_origins: List[str] = Field(["*"], env="CORS_ALLOWED_ORIGINS")

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
