# Local HiveMQ Setup and App Flow

This note documents the local HiveMQ broker setup, how the backend issues MQTT credentials, and how the Android app should connect.

## What was done
- Started HiveMQ locally via Docker Compose.
- Ensured port `1883` is free by stopping the local Mosquitto service.
- Started the backend API and validated the `/healthz` endpoint.
- Requested MQTT credentials from the backend and verified publish/subscribe.

## Local run flow
1. Start HiveMQ:
   ```bash
   docker compose -f backend/docker-compose.yml up -d
   ```
   - MQTT broker: `localhost:1883`
   - Control Center: `http://localhost:8080`

2. Run the backend:
   ```bash
   make -C backend dev
   ```

3. Confirm backend health:
   ```bash
   curl -H "Authorization: Bearer user_demo" http://localhost:8005/healthz
   ```

4. Fetch MQTT credentials (backend issues scoped access):
   ```bash
   curl -X POST -H "Authorization: Bearer user_demo" \
     http://localhost:8005/api/auth/mqtt
   ```

## Verified publish/subscribe
```bash
mosquitto_sub -h localhost -p 1883 \
  -u local_backend -P local_backend_password \
  -t "users/demo/devices/test" -C 1
```

```bash
mosquitto_pub -h localhost -p 1883 \
  -u local_backend -P local_backend_password \
  -t "users/demo/devices/test" -m "hello from codex"
```

## Use case and data flow
1. **App authenticates to backend** using an app token (`Authorization: Bearer user_<id>`).
2. **Backend issues MQTT credentials** via `POST /api/auth/mqtt`.
3. **App connects to HiveMQ** using the returned host, port, username, password, and client_id.
4. **App publishes/subscribes** only to the topics returned by the backend.
5. **Backend can rotate credentials** by shortening `MQTT_CREDENTIALS_TTL` or issuing new ones per request.

This keeps broker access scoped to a user and prevents hardcoding credentials into the app.

## Android app connection flow
The Android app should:
1. Call the backend for MQTT credentials:
   - Endpoint: `POST /api/auth/mqtt`
   - Header: `Authorization: Bearer user_<id>`
2. Use the response fields to configure the MQTT client:
   - `host`: broker host (local `localhost` for dev, cloud hostname in prod)
   - `port`: broker port (`1883` for non-TLS, `8883` for TLS)
   - `username`, `password`: broker credentials
   - `client_id`: MQTT client id
   - `topics`: allowed topic patterns to publish/subscribe
3. Connect and subscribe/publish within those topic scopes.

### Example (pseudocode)
```kotlin
val creds = backend.fetchMqttCreds(authToken)
mqttClient.connect(
  host = creds.host,
  port = creds.port,
  username = creds.username,
  password = creds.password,
  clientId = creds.clientId
)
mqttClient.subscribe("users/<id>/devices/#")
```

## Common pitfalls
- If port `1883` is busy, stop Mosquitto:
  ```bash
  sudo systemctl stop mosquitto
  ```
- If you remap broker ports, update `HIVEMQ_PORT` in `backend/.env`.
