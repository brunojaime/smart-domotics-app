from datetime import datetime, timedelta, timezone
import secrets
import uuid
from typing import Optional

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from passlib.context import CryptContext

from .config import settings
from .models import User, UserInDB
from .user_repository import BaseUserRepository, InMemoryUserRepository, SQLiteUserRepository


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login", auto_error=False)


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


def _create_token(data: dict, expires_delta: timedelta, token_type: str) -> str:
    to_encode = data.copy()
    to_encode.update({"exp": datetime.now(timezone.utc) + expires_delta, "type": token_type})
    return jwt.encode(to_encode, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def create_access_token(user_id: str) -> str:
    expire = timedelta(minutes=settings.access_token_expire_minutes)
    return _create_token({"sub": user_id}, expire, "access")


def create_refresh_token(user_id: str) -> str:
    expire = timedelta(minutes=settings.refresh_token_expire_minutes)
    return _create_token({"sub": user_id, "jti": secrets.token_hex(8)}, expire, "refresh")


def decode_token(token: str) -> dict:
    try:
        return jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
    except JWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token") from exc


def get_user_repository() -> BaseUserRepository:
    backend = settings.user_repository_backend.lower()
    if backend == "sqlite":
        return SQLiteUserRepository("users.db")
    return InMemoryUserRepository()


user_repo: BaseUserRepository = get_user_repository()


async def get_current_user(token: str | None = Depends(oauth2_scheme)) -> User:
    if not token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing token")

    payload = decode_token(token)
    if payload.get("type") != "access":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token payload")

    user = user_repo.get_by_id(user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return User(id=user.id, username=user.username, email=user.email)


def create_user(username: str, password: str, email: Optional[str] = None, google_sub: Optional[str] = None) -> UserInDB:
    user = UserInDB(
        id=str(uuid.uuid4()),
        username=username,
        email=email,
        hashed_password=hash_password(password),
        google_sub=google_sub,
    )
    return user_repo.create_user(user)
