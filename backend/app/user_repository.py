import sqlite3
from pathlib import Path
from typing import Optional

from .models import UserInDB


class BaseUserRepository:
    def create_user(self, user: UserInDB) -> UserInDB:
        raise NotImplementedError

    def get_by_username(self, username: str) -> Optional[UserInDB]:
        raise NotImplementedError

    def get_by_id(self, user_id: str) -> Optional[UserInDB]:
        raise NotImplementedError

    def get_by_google_sub(self, google_sub: str) -> Optional[UserInDB]:
        raise NotImplementedError


class InMemoryUserRepository(BaseUserRepository):
    def __init__(self) -> None:
        self._by_id: dict[str, UserInDB] = {}
        self._by_username: dict[str, str] = {}
        self._by_google_sub: dict[str, str] = {}

    def create_user(self, user: UserInDB) -> UserInDB:
        if user.username in self._by_username:
            raise ValueError("Username already exists")
        self._by_id[user.id] = user
        self._by_username[user.username] = user.id
        if user.google_sub:
            self._by_google_sub[user.google_sub] = user.id
        return user

    def get_by_username(self, username: str) -> Optional[UserInDB]:
        if username not in self._by_username:
            return None
        return self._by_id[self._by_username[username]]

    def get_by_id(self, user_id: str) -> Optional[UserInDB]:
        return self._by_id.get(user_id)

    def get_by_google_sub(self, google_sub: str) -> Optional[UserInDB]:
        if google_sub not in self._by_google_sub:
            return None
        return self._by_id[self._by_google_sub[google_sub]]


class SQLiteUserRepository(BaseUserRepository):
    def __init__(self, db_path: str | Path) -> None:
        self.db_path = Path(db_path)
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        conn = sqlite3.connect(self.db_path)
        try:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    email TEXT,
                    hashed_password TEXT NOT NULL,
                    google_sub TEXT UNIQUE
                )
                """
            )
            conn.commit()
        finally:
            conn.close()

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path)

    def create_user(self, user: UserInDB) -> UserInDB:
        conn = self._connect()
        try:
            conn.execute(
                "INSERT INTO users (id, username, email, hashed_password, google_sub) VALUES (?, ?, ?, ?, ?)",
                (user.id, user.username, user.email, user.hashed_password, user.google_sub),
            )
            conn.commit()
            return user
        except sqlite3.IntegrityError as exc:
            raise ValueError("User already exists") from exc
        finally:
            conn.close()

    def _row_to_user(self, row: tuple[str, str, str | None, str, str | None]) -> UserInDB:
        return UserInDB(id=row[0], username=row[1], email=row[2], hashed_password=row[3], google_sub=row[4])

    def get_by_username(self, username: str) -> Optional[UserInDB]:
        conn = self._connect()
        try:
            cur = conn.execute("SELECT id, username, email, hashed_password, google_sub FROM users WHERE username=?", (username,))
            row = cur.fetchone()
            return self._row_to_user(row) if row else None
        finally:
            conn.close()

    def get_by_id(self, user_id: str) -> Optional[UserInDB]:
        conn = self._connect()
        try:
            cur = conn.execute("SELECT id, username, email, hashed_password, google_sub FROM users WHERE id=?", (user_id,))
            row = cur.fetchone()
            return self._row_to_user(row) if row else None
        finally:
            conn.close()

    def get_by_google_sub(self, google_sub: str) -> Optional[UserInDB]:
        conn = self._connect()
        try:
            cur = conn.execute(
                "SELECT id, username, email, hashed_password, google_sub FROM users WHERE google_sub=?",
                (google_sub,),
            )
            row = cur.fetchone()
            return self._row_to_user(row) if row else None
        finally:
            conn.close()

