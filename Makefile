.PHONY: backend-dev backend-dev-sqlite frontend

## Run the FastAPI backend using the backend Makefile defaults
backend-dev:
	make -C backend dev

## Run the FastAPI backend with a SQLite URL and optional Alembic migrations
backend-dev-sqlite:
	cd backend && \
		export DATABASE_URL=$${DATABASE_URL:-sqlite:///./app.db} && \
		if [ -f alembic.ini ]; then \
			DATABASE_URL=$${DATABASE_URL:-sqlite:///./app.db} alembic upgrade head; \
		else \
			echo "Skipping migrations: alembic.ini not found"; \
		fi && \
		uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

## Build the Android frontend
frontend:
	./gradlew :app:assembleDebug
