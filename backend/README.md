# Broker backend (HiveMQ-ready)

A lightweight FastAPI service that issues scoped MQTT credentials and manages device provisioning for the Smart Domotics app.

## Branch status
- Work for the broker is tracked on the `feature/hivemq-broker` branch (not `main`).
- Keep local changes on this branch and push a same-named branch remotely when ready to open a PR.

## Quickstart
1. Start HiveMQ locally (MQTT on `1883`, Control Center on `8080`):
   ```bash
   docker compose -f backend/docker-compose.yml up -d
   # Visit http://localhost:8080 to confirm HiveMQ is up
   ```
2. Create `.env` from the sample (pre-filled for local dev):
   ```bash
   cp backend/.env.example backend/.env
   ```
3. Install deps and run locally:
   ```bash
   make -C backend install
   make -C backend dev
   ```
4. Call the API with an app token (prefix defaults to `user_`):
   ```bash
   curl -H "Authorization: Bearer user_demo" http://localhost:8000/healthz
   ```

### Useful endpoints
- `POST /api/auth/mqtt`: returns HiveMQ host/port and scoped credentials for the current user.
- `POST /api/devices`: registers a device for the user and returns allowed topics.
- `GET /api/devices`: lists user devices with their topic scopes.
- `DELETE /api/devices/{id}`: removes a device.

### Smoke test the spatial hierarchy
Use the nested `/api/v1` routes to create a hierarchy before wiring up the mobile client:

```bash
# Create a location
curl -s -X POST http://localhost:8000/api/v1/locations \
  -H "Content-Type: application/json" \
  -d '{"name": "HQ"}' | jq .

# Create a building under that location
curl -s -X POST http://localhost:8000/api/v1/locations/<location_id>/buildings \
  -H "Content-Type: application/json" \
  -d '{"name": "Tower"}' | jq .

# Add a zone and device inside the building
curl -s -X POST http://localhost:8000/api/v1/locations/<location_id>/buildings/<building_id>/zones \
  -H "Content-Type: application/json" \
  -d '{"name": "Lobby"}' | jq .

curl -s -X POST http://localhost:8000/api/v1/locations/<location_id>/buildings/<building_id>/zones/<zone_id>/devices \
  -H "Content-Type: application/json" \
  -d '{"device_id": "dev-1", "name": "Env Sensor"}' | jq .
```

### MQTT connect example
Fetch credentials from the API, then connect with your MQTT client using the returned values.

```bash
# Get creds (local backend)
curl -s -H "Authorization: Bearer user_demo" \
  http://localhost:8000/api/auth/mqtt | jq .
```

```text
host:     use as broker host (local or cloud)
port:     use as broker port (1883 non-TLS, 8883 TLS)
username: use as MQTT username
password: use as MQTT password
client_id: use as MQTT client id
topics:   publish/subscribe within these scopes
```

### Configuration
Set these environment variables (see `.env.example`):
- `HIVEMQ_HOST=localhost`, `HIVEMQ_PORT=1883`, `HIVEMQ_USERNAME=local_backend`, `HIVEMQ_PASSWORD=local_backend_password`
- `APP_TOKEN_PREFIX=user_`, `APP_JWT_SECRET=dev_super_secret_change_later`
- `MQTT_CREDENTIALS_TTL=86400`
- `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173` (comma-separated origins for the app frontend)

The API uses a simple token convention (`user_<id>`) for now; swap `get_current_user` with your real auth validation when ready.
