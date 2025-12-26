# Smart Domotics App

An Android application for smart home and industrial automation control, built with Kotlin and Jetpack Compose.

## Features

- Modern UI with Jetpack Compose and Material Design 3
- Network communication for IoT device control (Retrofit, OkHttp)
- Local data persistence with Room Database
- Coroutines for asynchronous operations
- Navigation support

## Requirements

- Android SDK 24+ (Android 7.0 Nougat or higher)
- JDK 8 or higher
- Gradle 8.2

## Development Setup

### Environment

- Copy the backend sample environment and customize secrets as needed:

  ```bash
  cp backend/.env.example backend/.env
  ```

### VS Code

1. Install the recommended extensions:
   - Kotlin Language
   - Java Extension Pack
   - Gradle for Java

2. Make sure you have Android SDK installed and `ANDROID_HOME` environment variable set

3. Open the project in VS Code

4. Build the project:
   ```bash
   ./gradlew build
   ```

### Make targets

The repository Makefile provides shortcuts for common workflows:

- `make backend-dev`: start the FastAPI backend using the backend Makefile defaults.
- `make backend-dev-sqlite`: run SQLite-based development with optional Alembic migrations (if configured) before starting the backend.
- `make frontend`: assemble the Android app debug build.

### Android Studio (Alternative)

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the project directory
4. Wait for Gradle sync to complete

## Project Structure

```
smart-domotics-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   └── java/com/domotics/smarthome/
│   │   │       └── entities/
│   │   │           ├── Location.kt
│   │   │           ├── Building.kt
│   │   │           ├── Zone.kt
│   │   │           ├── Area.kt
│   │   │           ├── Device.kt (abstract)
│   │   │           ├── Lighting.kt
│   │   │           └── Sensor.kt
│   │   └── test/
│   │       └── java/com/domotics/smarthome/
│   │           └── entities/
│   │               ├── LocationTest.kt
│   │               ├── BuildingTest.kt
│   │               ├── ZoneTest.kt
│   │               ├── AreaTest.kt
│   │               └── DeviceTest.kt
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Architecture

### Domain Entities

The app follows a domain-driven design with the following core entities:

- **Location**: Geographical coordinates (latitude, longitude) with optional reference
- **Building**: Top-level entity containing zones, with a name, location, and description
- **Zone**: Represents floors or areas within a building, contains devices
- **Area**: Defines physical spaces with optional square meters measurement
- **Device** (abstract): Base class implementing publisher/subscriber pattern for device state changes
  - **Lighting**: Controls brightness (0-100) and on/off state
  - **Sensor**: Monitors various metrics (temperature, humidity, motion, etc.)

All entities include:
- UUID-based identification
- Input validation
- Comprehensive unit test coverage (54 tests)

## Permissions

The app requests the following permissions for IoT functionality:
- Internet access
- Network state access
- WiFi control
- Bluetooth connectivity
- Location (required for Bluetooth scanning)

## Dependencies

- **Jetpack Compose**: Modern declarative UI framework
- **Material 3**: Latest Material Design components
- **Navigation Compose**: In-app navigation
- **Retrofit**: REST API client
- **OkHttp**: HTTP client
- **Room**: Local database
- **Coroutines**: Asynchronous programming

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Local Broker Setup (Physical Device)

To run the app on a real phone, make sure the backend and broker are reachable on your LAN.

1. Start HiveMQ and the backend:
   ```bash
   docker compose -f backend/docker-compose.yml up -d
   make -C backend dev
   ```
2. Set the backend to advertise your LAN IP in `backend/.env`:
   ```
   HIVEMQ_HOST=192.168.1.28
   HIVEMQ_PORT=1883
   ```
3. Configure the app base URL via Gradle properties (local machine only):
   ```
   ~/.gradle/gradle.properties
   API_BASE_URL=http://192.168.1.28:8005
   ```
4. Update cleartext allowance for your LAN IP:
   - `app/src/main/res/xml/network_security_config.xml`
   - Replace `192.168.1.28` with your current LAN IP.
5. Rebuild/install the app:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Additional documentation

- [Device provisioning architecture tasks (strategy + adapter)](docs/device-provisioning-architecture-tasks.md)
- [Device provisioning architecture (implementation overview)](docs/device-provisioning-architecture.md)

## License

TBD
