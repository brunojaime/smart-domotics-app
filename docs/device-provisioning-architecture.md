# Device provisioning architecture (strategy + adapter)

This document describes the concrete implementation for the provisioning strategy/adapter design. The goal is to make device onboarding pluggable per protocol while keeping transport-specific behavior isolated.

## Components
- **ProvisioningRequest**: Pydantic model that captures device id, type, requested strategy/adapter, declared capabilities, and transport payload.
- **ProvisioningStrategy**: Defines lifecycle steps for a device family and the default adapter to use. Two strategies ship by default:
  - `wifi`: validates Wi-Fi credentials and sends onboarding payloads through a Wi-Fi capable adapter.
  - `zigbee`: emits a join request and expects a Zigbee-capable adapter to deliver the payload.
- **ProvisioningAdapter**: Adapter interface that hides SDK/transport details. The default adapters (`wifi_local`, `zigbee_local`) are in-memory and capability-checked for tests. New adapters can wrap HTTP, BLE, vendor clouds, etc., while keeping strategies unchanged.
- **ProvisioningRegistry**: Registry for adapters and strategies. Strategy selection falls back to `device_type` when an explicit `strategy` is not provided, and adapter selection falls back to the strategy’s default.
- **ProvisioningService**: Orchestrates a provisioning run, maps failures to rollbacks, and summarizes step outcomes.
- **Provisioning API**: `POST /api/v1/provisioning/devices/{device_id}` runs the pipeline for the authenticated user and returns each lifecycle step.

## API schema
Request body:
```json
{
  "device_id": "sensor-1",
  "device_type": "wifi",
  "capabilities": ["wifi"],
  "payload": {"ssid": "Lab", "password": "labpass"}
}
```

Response body:
```json
{
  "device_id": "sensor-1",
  "strategy": "wifi",
  "adapter": "wifi_local",
  "status": "succeeded",
  "steps": [
    {"stage": "detect", "status": "succeeded", "detail": "Adapter connected"},
    {"stage": "precheck", "status": "succeeded", "detail": "Wi-Fi credentials validated"},
    {"stage": "provision", "status": "succeeded", "detail": "Payload delivered"},
    {"stage": "confirm", "status": "succeeded", "detail": "Provisioning confirmed"}
  ]
}
```

Errors include explicit messages for unsupported strategies/adapters or device id mismatches, and failed runs automatically append a rollback step.

## Testing guidance
- Unit tests validate registry selection, lifecycle ordering, and rollback behavior (`backend/tests/test_provisioning.py`).
- Integration tests ensure the HTTP endpoint returns lifecycle events for authenticated users.
- Adapter contract tests should verify payload mapping and timeouts when new adapters are introduced.
- The Android client now targets the same endpoint via `ProvisioningApiService` and `BackendProvisioningRepository`,
  with unit coverage in `app/src/test/kotlin/com/domotics/smarthome/provisioning/BackendProvisioningRepositoryTest.kt`.

## Extension points
- Register new strategies in `ProvisioningRegistry` with their required capabilities and default adapter.
- Implement adapters by subclassing `ProvisioningAdapter` and adding them to the registry.
- Extend `ProvisioningRequest` payloads to include new transport fields (e.g., BLE pairing codes) while keeping strategies and adapters decoupled.
