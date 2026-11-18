# Device Compatibility Documentation — HARDIK BUDS PRO ULTRA

HARDIK BUDS PRO ULTRA uses a modular `BrandIntegrationRegistry` (`app/src/main/java/com/example/data/bluetooth/BrandIntegrationRegistry.kt`) so each earbuds brand maps cleanly to documented Bluetooth standards vs. proprietary OEM SDK status.

## Supported Standard Capabilities (All Bluetooth Audio Brands)
- **Bluetooth Classic & BLE Discovery / Bonding / Reconnection** (`BluetoothA2dp`, `BluetoothHeadset`, `BluetoothGatt`)
- **Standard GATT Battery Service (`0x180F` / `0x2A19`) & Android HFP Battery Metadata**
- **Standard GATT Device Information Service (`0x180A` — Firmware Revision `0x2A26`, Manufacturer `0x2A29`, Model `0x2A24`)**
- **BLE Immediate Alert (`0x1802`) + Stereo A2DP Audio Finder Chime**
- **Android Hardware AudioFX (`Equalizer`, `BassBoost`, `Virtualizer`), Stereo Channel Test & Mic Test**
- **Phone Last-Known Location on Disconnect & Keystore Security Vault**

## Brand Integration Matrix

| Brand | Identification Method | Standard Profiles & AudioFX | Proprietary Controls (ANC / Touch / In-Ear / Case %) |
| :--- | :--- | :--- | :--- |
| **OnePlus Buds** | Name + SIG Company ID (`0x072F`, `0x058D`) | Supported | Pending Device Integration (HeyMelody OEM SDK required) |
| **Samsung Galaxy Buds** | Name + SIG Company ID (`0x0075`) | Supported | Pending Device Integration (Galaxy Wearable SDK required) |
| **Apple AirPods** | Name + SIG Company ID (`0x004C`) | Supported | Pending Device Integration (Apple AACP proprietary) |
| **Realme Buds** | Device Name Pattern | Supported | Pending Device Integration (Realme Link OEM SDK required) |
| **Oppo Enco** | Name + SIG Company ID (`0x079A`) | Supported | Pending Device Integration (HeyMelody OEM SDK required) |
| **Xiaomi Redmi Buds** | Name + SIG Company ID (`0x038F`) | Supported | Pending Device Integration (Mi Earbuds SDK required) |
| **JBL** | Name + SIG Company ID (`0x0057`) | Supported | Pending Device Integration (Harman/JBL SDK required) |
| **Sony** | Name + SIG Company ID (`0x012D`) | Supported | Pending Device Integration (Sony Tandem SDK required) |
| **boAt** | Device Name Pattern | Supported | Pending Device Integration (boAt Hearables SDK required) |
| **Noise** | Device Name Pattern | Supported | Pending Device Integration (NoiseFit Audio SDK required) |
| **Other Standard Bluetooth** | Fallback Adapter | Supported | Not Supported on This Device |

> **Policy Note:** HARDIK BUDS PRO ULTRA does not reverse-engineer encrypted or undocumented proprietary vendor protocols. Features requiring OEM SDK authorization are honestly surfaced in the UI as **Pending Device Integration** or **Not Supported on This Device**.
