# Permission Documentation — HARDIK BUDS PRO ULTRA

HARDIK BUDS PRO ULTRA follows **Google Play Developer Program** least-privilege guidelines. All hardware permissions are optional (`android:required="false"`) and requested with clear in-app context.

| Permission | Android API Level | Purpose | Runtime Flow |
| :--- | :--- | :--- | :--- |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | API 24–30 (`maxSdkVersion="30"`) | Classic Bluetooth & BLE discovery, bonding, profile monitoring on Android 8.0–11 | Install-time (paired with runtime Location for scan) |
| `BLUETOOTH_SCAN` | API 31+ (Android 12+) | BLE scanning and RSSI proximity updates for Find My Earbuds | Runtime (`Nearby devices` prompt) |
| `BLUETOOTH_CONNECT` | API 31+ (Android 12+) | Reading bonded earbuds, A2DP/HFP status, GATT services (`0x180F`, `0x180A`, `0x1802`) | Runtime (`Nearby devices` prompt) |
| `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | All supported | 1) BLE scan requirement on Android 8–11; 2) Recording phone's last-known coordinates on earbuds disconnect for Find My Earbuds | Runtime prompt with explicit phone-location notice |
| `POST_NOTIFICATIONS` | API 33+ (Android 13+) | Connection, disconnection, low battery, Bluetooth off, and unauthorized PIN attempt alerts | Runtime notification prompt |
| `RECORD_AUDIO` | All supported | Live Audio Studio Microphone Test & Bluetooth SCO/BLE mic level diagnostics | Runtime prompt when starting Mic Test |
| `MODIFY_AUDIO_SETTINGS` | All supported | Routing microphone test through Bluetooth SCO / Communication device | Install-time normal permission |
| `VIBRATE` | All supported | Haptic feedback and security alert notification vibration | Install-time normal permission |

## Excluded Broad Permissions
- **No `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*`**: Complies with Play Policy scoped access.
- **No `ACCESS_BACKGROUND_LOCATION`**: Location is only sampled in foreground/active broadcast callbacks when granted.
