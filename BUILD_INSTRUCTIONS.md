# Build Instructions — HARDIK BUDS PRO ULTRA

## Prerequisites
- **Android Studio**: Ladybug / Meerkat or newer
- **JDK**: Java 11+ (Java 17 recommended by AGP 9.x)
- **Android SDK**: Compile SDK 36, Target SDK 36, Min SDK 24 (supports Android 8.0 Oreo API 26 through Android 16)
- **Gradle**: Uses the project Gradle configuration (`gradle` CLI or Gradle Wrapper in Android Studio)

## Building Debug APK
From the project root:
```bash
gradle :app:assembleDebug
```
Output APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Building Release APK / AAB
Release builds use environment-configured signing credentials (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) defined in `app/build.gradle.kts`:
```bash
export KEYSTORE_PATH=/path/to/your-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password

gradle :app:assembleRelease
# Or Android App Bundle:
gradle :app:bundleRelease
```
Output Release APK path:
`app/build/outputs/apk/release/app-release.apk`

## Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
Test XML/HTML reports are generated under `app/build/reports/tests/testDebugUnitTest/`.
