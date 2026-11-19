# HARDIK BUDS PRO ULTRA (`HARDIK-BUDS-PRO`)

**HARDIK BUDS PRO ULTRA** is a production-oriented Android Bluetooth & BLE earbuds companion application built with **Kotlin**, **Jetpack Compose**, **Clean Architecture + MVVM**, **Room**, **DataStore**, **Android AudioFX**, and **Android Keystore + Biometrics**.

> © 2026 HARDIK BUDS PRO. All Rights Reserved.

---

## Key Features & Engineering Honesty

1. **Dual Luxury Themes & Adaptive UI**
   - **iOS Liquid Glass** (translucent frosted glass cards, specular highlights, Dynamic Island status pill, iOS-inspired connection popup)
   - **Black + Silver Luxury** (obsidian chassis with brushed platinum borders)
   - Light, Dark, and System brightness modes + 5 accent palettes + custom bundled fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`) + adaptive `NavigationBar` / `NavigationRail`.

2. **Real Android Bluetooth & BLE Connectivity**
   - BLE scanning (`BluetoothLeScanner`) with 15-second timeout and error handling
   - Classic Bluetooth discovery (`BluetoothAdapter.startDiscovery`)
   - Paired/bonded device listing and bonding (`BluetoothDevice.createBond()`)
   - Real A2DP, HFP (`BluetoothHeadset`), and BLE GATT (`0x180F` Battery, `0x180A` Device Info, `0x1802` Immediate Alert) monitoring
   - Never displays fake connected devices or fabricated battery percentages.

3. **Smart Battery Dashboard**
   - Displays combined/headset or GATT battery telemetry when exposed by connected earbuds
   - Clearly marks unavailable Left/Right/Case individual sensors as `N/A`
   - Low battery warnings and Room-persisted battery telemetry history chart.

4. **Audio Studio (Real Android DSP)**
   - Hardware 10-band EQ mapped to `android.media.audiofx.Equalizer`
   - `BassBoost`, `Virtualizer` Spatial Audio, Music / Gaming / Movie modes, and Room-persisted Custom Presets
   - Real `AudioTrack` Stereo Channel Test (Left / Right / Stereo)
   - Real `AudioRecord` Live Microphone Test with Bluetooth SCO/Communication routing and dBFS meter.

5. **Smart Earbud Controls & Modular Brand Registry**
   - Modular adapters for OnePlus, Samsung, Apple, Realme, Oppo, Xiaomi, JBL, Sony, boAt, Noise, and Standard Bluetooth
   - Strictly gates proprietary features (ANC, Transparency, Touch Remapping, In-Ear Detection, Firmware OTA) as `Supported`, `Pending Device Integration`, or `Not Supported on This Device`.

6. **Find My Earbuds**
   - Records last-known **phone** location via `LocationManager` on connection/disconnection with an explicit transparency disclaimer distinguishing phone location from earbud hardware GPS
   - Left / Right / Both pulsed finder sound + BLE Immediate Alert (`0x1802`) + live BLE RSSI meter.

7. **Keystore Security Vault & Smart Notifications**
   - Android Keystore AES-256-GCM encrypted salt + salted SHA-256 4/6-digit PIN
   - 5-attempt rate-limiting lockout, recovery phrase reset, and AndroidX `BiometricPrompt`
   - Owner device registration, security audit log, and Android 8+ Notification Channels with custom sound picker.

8. **Gemini AI Acoustic Studio (Firebase AI Logic + App Check)**
   - Powered by **Firebase AI Logic** (`Firebase.ai`, `gemini-3.8-flash`) with **Firebase App Check** (`DebugAppCheckProviderFactory` in debug, `RecaptchaAppCheckProviderFactory` in release)
   - **AI 10-Band EQ Designer**: Generates structured JSON 10-band EQ curves, BassBoost, and Spatializer settings applied directly to Android `AudioFX` DSP and saved to Room
   - **Earbuds & Acoustics Coach**: Streaming Q&A grounded with live connected earbuds telemetry and AudioFX state.

---

## Documentation Index
- [Build Instructions](BUILD_INSTRUCTIONS.md)
- [Permission Documentation](PERMISSIONS.md)
- [Device Compatibility Matrix](DEVICE_COMPATIBILITY.md)
- [Test Report](TEST_REPORT.md)
- [Version 1.0.0 Release Notes](RELEASE_NOTES.md)
