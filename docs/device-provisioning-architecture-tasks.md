# Device provisioning architecture tasks (strategy + adapter)

This checklist breaks down the work to implement a flexible device provisioning flow that separates provisioning **strategy** (per device family) from the **adapter** that speaks to external provisioning APIs or transports.

## Goals
- Allow pluggable provisioning strategies per device family or protocol without rewriting the rest of the stack.
- Introduce adapters so external SDKs/APIs (e.g., Wi‑Fi onboarding, Zigbee join, vendor cloud registration) are isolated behind a unified interface.
- Cover backend, mobile, and infrastructure pieces with tests and documentation so the provisioning path stays observable and stable.

## Core architecture
- [x] Define a `ProvisioningStrategy` contract describing lifecycle steps (detect, pre-check, provision, confirm, rollback) and expected payloads/events.
- [x] Model strategy selection (factory or registry) based on device metadata (type, capabilities, vendor) with sensible defaults and error reporting for unsupported devices.
- [x] Design an `Adapter` interface to abstract transport/SDK specifics (e.g., HTTP, BLE, Zigbee, vendor cloud) and keep it separate from business rules in the strategy.
- [ ] Document a sequence diagram showing how strategy and adapter interact during provisioning and where audit/telemetry hooks are emitted.

## Backend tasks
- [x] Add domain models/DTOs for provisioning requests and results (including transient states like `PENDING_NETWORK`, `WAITING_FOR_DEVICE`, `FAILED_VALIDATION`).
- [x] Implement strategy registration/discovery in the backend service layer; wire it through dependency injection to make strategies testable.
- [x] Create adapters for each supported protocol/vendor using the new adapter interface; ensure timeouts/retries are centralized and configurable.
- [x] Expose provisioning endpoints (e.g., `POST /api/v1/devices/{device_id}/provision`) that delegate to the strategy/adapter pipeline and stream progress updates (SSE/WebSocket) when available.
- [ ] Add audit logging and metrics for each lifecycle stage to aid observability and troubleshooting.

## Mobile/Client tasks
- [ ] Update client-side models to align with backend provisioning DTOs and status enums.
- [ ] Add a provisioning coordinator that picks the right strategy (matching backend contract) and feeds progress to the UI.
- [ ] Implement adapters that wrap platform APIs/SDKs (e.g., Wi‑Fi credentials, BLE scanning) behind the shared adapter interface to keep UI logic simple.
- [ ] Provide retry/rollback flows in the UI (e.g., restart pairing, forget credentials) hooked into the strategy lifecycle.

## Testing
- [x] Unit-test strategy selection and lifecycle transitions with fakes/mocks for adapters to ensure deterministic behavior.
- [x] Add contract tests for each adapter to validate request/response mapping, timeouts, and error translation.
- [x] Write integration tests for the provisioning endpoint that simulate end-to-end flows (happy path, missing capabilities, adapter failure with rollback).
- [ ] Add UI/acceptance tests that exercise the provisioning coordinator and show progress states end-to-end.

## Documentation and operational readiness
- [ ] Document new public APIs (backend and client) including payloads, enums, and expected error codes.
- [ ] Add runbooks/playbooks for common provisioning failures (network timeout, device unreachable, vendor API quota) and how to capture logs/metrics.
- [ ] Provide migration guidance for existing provisioning flows (what to deprecate, feature flags/kill switches, rollout order).
- [ ] Include architecture diagrams (strategy/adapter layering, data flow, and extension points) in the docs folder and link them from the README.
