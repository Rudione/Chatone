# Build Instructions for Chatone

## Important: Gradle Wrapper Setup

This project requires the Gradle Wrapper JAR file which is not included in the repository.

### Setup Steps:

1. **Install Gradle** (if not already installed):
   - Download from: https://gradle.org/install/
   - Or use package manager:
     - macOS: `brew install gradle`
     - Linux: `sudo apt install gradle` or `sudo dnf install gradle`
     - Windows: Use Chocolatey `choco install gradle`

2. **Generate Gradle Wrapper**:
   ```bash
   cd Chatone
   gradle wrapper --gradle-version 8.11.1
   ```

3. **Verify Wrapper**:
   ```bash
   ./gradlew --version
   ```

## Building the Project

### Desktop (All Platforms)

```bash
./gradlew :composeApp:run
```

### Android

1. Set up `local.properties`:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

2. Build:
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```

3. Or open in Android Studio and run

### iOS

1. Install Xcode from App Store
2. Run:
   ```bash
   ./gradlew :composeApp:iosSimulatorArm64Test
   ```
3. Or open `iosApp/iosApp.xcodeproj` in Xcode

## Configuration

### Twitch API Credentials

Edit `composeApp/src/commonMain/kotlin/io/rudione/chatone/util/AppConfig.kt`:

```kotlin
const val TWITCH_CLIENT_ID = "your_client_id_here"
const val TWITCH_CLIENT_SECRET = "your_client_secret_here"
```

Get credentials from: https://dev.twitch.tv/console/apps

## Troubleshooting

### "Gradle wrapper not found"
- Run: `gradle wrapper --gradle-version 8.11.1`

### "Android SDK not found"
- Set `sdk.dir` in `local.properties`

### "Cannot find JDK"
- Install JDK 17 or higher
- Set JAVA_HOME environment variable

## Dependencies

- JDK 17+
- Gradle 8.11.1
- Android SDK (for Android builds)
- Xcode (for iOS builds)
