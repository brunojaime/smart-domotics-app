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
│   │   └── main/
│   │       ├── kotlin/com/domotics/smarthome/
│   │       │   ├── MainActivity.kt
│   │       │   └── ui/theme/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

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
```

## License

TBD
