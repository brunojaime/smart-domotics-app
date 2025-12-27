# Frontend ↔ Backend wiring tasks for spatial hierarchy

The Android client currently calls flat `/api/*` endpoints with payloads that do not match the FastAPI backend. This checklist captures the work needed to align creation flows for locations, buildings, zones, areas, and devices.

## Shared foundations
- [ ] Point the mobile `SmartHomeApiService` base path to the backend prefix (`/api/v1`) and carry the nested path parameters required by the backend routers.
- [ ] Update DTOs to mirror backend contracts (names and shapes) instead of the current latitude/longitude/floor/area fields that the backend does not accept.
- [ ] Ensure repositories re-expose the new method signatures (IDs flowing down from parents) so UI layers can call them.

## Locations
- [ ] Replace the current latitude/longitude-based create/update payload with the backend’s `name`-only contract and map the `/api/v1/locations` endpoints (`GET`, `POST`, `PUT`, `DELETE`).
- [ ] Propagate location IDs from API responses so building creation screens can use them as path parameters.

## Buildings
- [ ] Swap the flat `/api/buildings` calls for the nested `/api/v1/locations/{location_id}/buildings` routes.
- [ ] Adjust the create/update request to the backend shape (`{"name": "..."}`) and drop the unused `description`/`locationId` fields, or extend the backend if those fields are required by the UI.
- [ ] Make repository methods accept a `locationId` argument to match the nested routes.

## Zones
- [ ] Migrate to `/api/v1/locations/{location_id}/buildings/{building_id}/zones` for listing and creating zones.
- [ ] Align the payload to the backend’s `name`-only structure; if floor or zone type is needed in the UI, add backend support and update DTOs consistently.
- [ ] Bubble both `locationId` and `buildingId` through repository/UI flows for zone creation and updates.

## Areas
- [ ] Use `/api/v1/locations/{location_id}/buildings/{building_id}/zones/{zone_id}/areas` for CRUD operations.
- [ ] Match the backend payload (`{"name": "..."}`) and remove the optional `squareMeters` field unless backend support is added.
- [ ] Ensure zone selection passes `zoneId` into repository calls.

## Devices (per zone)
- [ ] Replace the standalone `/devices/register` call with `/api/v1/locations/{location_id}/buildings/{building_id}/zones/{zone_id}/devices`.
- [ ] Update the request body to the backend schema (`{"device_id": "...", "name": "..."}`) and consume the `device_id` returned by the service.
- [ ] Thread `locationId` → `buildingId` → `zoneId` into the device creation UI so the correct nested endpoint is hit.

## Testing
- [ ] Add integration tests (or mock-backed unit tests) in the Android app to verify that repository methods hit the expected URLs and serialize request bodies per backend expectations.
- [ ] Smoke-test the FastAPI routes with `curl`/`HTTPie` examples in the README once the client payloads are aligned.

# Device provisioning architecture (strategy + adapter)

This plan documents a transport-agnostic provisioning flow that hides hardware and transport details from the user experience.

## Non-negotiables
- Do not put transport logic in UI.
- Do not branch UI per transport.
- Do not let firmware or transport details leak upward.
- Do not collapse Strategy and Adapter into one class.

## Core patterns
- **Strategy** is the core pattern: owns discovery, retries, fallback, and the provisioning state machine.
- **Adapter** is the supporting pattern: wraps transports and exposes a normalized API.
- Strategy and Adapter solve different problems and must coexist.

## Responsibilities
### Mobile app
- Detect device candidates via discovery.
- Auto-select provisioning transport (prefer BLE, fallback to Soft AP).
- Drive a single UX flow for all devices.
- Handle retries and fallback without exposing transport details.

### Firmware
- Implement Soft AP provisioning endpoint.
- Implement BLE provisioning service (when available).
- Connect to Wi-Fi and broker using supplied credentials.

### Backend
- Device registry and ownership binding.
- Auth tokens and MQTT credential minting (scoped, short-lived).
- OTA metadata.
- Never handles Wi-Fi credentials.

## Mobile architecture
### Strategy layer
- Owns discovery orchestration and provisioning flow.
- Implements retry/backoff and fallback semantics.
- Emits a normalized provisioning state machine.

### Adapter layer
- Wraps BLE, HTTP/Soft AP, or other transports.
- Exposes a normalized API to the Strategy.

### Strategy state model
- `idle → discovering → connecting → sending_credentials → awaiting_online → done | failed`
- Failure states should include reason codes for retries.

### Adapter API (example)
- `discover(): Flow<DiscoveryResult>`
- `connect(target): Result<Unit>`
- `sendWifiCredentials(ssid, password, deviceId): Result<Unit>`
- `sendMqttCredentials(creds): Result<Unit>`
- `observeStatus(): Flow<ProvisioningStatus>`

## Provisioning flow (transport-agnostic)
1. Start discovery.
2. If BLE candidates found, use BLE adapter; else use Soft AP adapter.
3. Send Wi-Fi credentials directly to device.
4. Request MQTT credentials from backend; send to device.
5. Device connects to broker.
6. Confirm device online and allow zone/leaf assignment.

## Backend contracts
- `POST /api/devices/pair` → bind ownership + return scoped MQTT credentials.
- `POST /api/devices/{id}/assign` → link device to zone/leaf.
- `GET /api/devices` → list owned devices.

## UX requirements
Show:
- Connecting to device
- Sending Wi-Fi info
- Device online

Do not show:
- Transport type
- Hardware type
