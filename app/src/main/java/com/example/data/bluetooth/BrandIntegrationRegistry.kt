package com.example.data.bluetooth

import com.example.domain.model.CapabilityStatus
import com.example.domain.model.DeviceCapabilityMatrix
import com.example.domain.model.EarbudBrand
import com.example.domain.model.EarbudFeatureId
import com.example.domain.model.FeatureCapability

interface BrandDeviceAdapter {
    val brand: EarbudBrand
    fun matches(deviceName: String?, manufacturerId: Int?, manufacturerString: String?): Boolean
    fun buildCapabilities(
        hasGattBattery: Boolean,
        hasCombinedBattery: Boolean,
        hasFirmwareRevision: Boolean,
        hasImmediateAlert: Boolean,
        eqSupported: Boolean
    ): DeviceCapabilityMatrix
}

object BrandIntegrationRegistry {

    private val adapters: List<BrandDeviceAdapter> = listOf(
        VendorBrandAdapter(
            brand = EarbudBrand.APPLE,
            nameKeywords = listOf("airpods", "beats"),
            companyIds = setOf(0x004C), // Apple Inc. Bluetooth SIG Company ID
            vendorSdkNote = "Apple proprietary AACP / Continuity BLE protocol requires Apple MFi / iOS entitlements; standard A2DP/HFP & Android AudioFX remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.SAMSUNG,
            nameKeywords = listOf("galaxy buds", "samsung buds", "sm-r1", "sm-r4", "sm-r5", "sm-r6"),
            companyIds = setOf(0x0075), // Samsung Electronics
            vendorSdkNote = "Samsung proprietary RFCOMM control protocol requires Galaxy Wearable Manager plugin; standard A2DP/HFP & Android AudioFX remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.ONEPLUS,
            nameKeywords = listOf("oneplus buds", "oneplus nord buds", "bullets wireless"),
            companyIds = setOf(0x072F, 0x058D),
            vendorSdkNote = "OnePlus HeyMelody / Fast Pair proprietary GATT commands require official OEM SDK authorization; standard profiles remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.REALME,
            nameKeywords = listOf("realme buds", "buds air", "buds t", "buds q"),
            companyIds = setOf(),
            vendorSdkNote = "Realme Link proprietary control service requires official OEM SDK; standard Bluetooth audio & EQ remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.OPPO,
            nameKeywords = listOf("oppo enco", "enco air", "enco free", "enco x", "enco buds"),
            companyIds = setOf(0x079A),
            vendorSdkNote = "Oppo Enco HeyMelody protocol requires official OEM SDK authorization; standard Bluetooth audio & EQ remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.XIAOMI,
            nameKeywords = listOf("redmi buds", "xiaomi buds", "mi true wireless", "poco pods", "airdots"),
            companyIds = setOf(0x038F), // Xiaomi Inc.
            vendorSdkNote = "Xiaomi Mi Earbuds / MMA proprietary protocol requires official Xiaomi Earbuds SDK; standard Bluetooth audio & EQ remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.SONY,
            nameKeywords = listOf("wf-1000", "wh-1000", "linkbuds", "sony wf", "sony wh", "sony c"),
            companyIds = setOf(0x012D), // Sony Corporation
            vendorSdkNote = "Sony Headphones Connect Tandem protocol requires Sony SDK partnership; standard A2DP/HFP/GATT & Android AudioFX remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.JBL,
            nameKeywords = listOf("jbl tune", "jbl live", "jbl tour", "jbl wave", "jbl reflect", "jbl vibe"),
            companyIds = setOf(0x0057), // Harman International
            vendorSdkNote = "JBL Headphones App control protocol requires Harman SDK; standard Bluetooth audio & EQ remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.BOAT,
            nameKeywords = listOf("airdopes", "boat", "nirvana ion", "immortal"),
            companyIds = setOf(),
            vendorSdkNote = "boAt Hearables app protocol requires Imagine Marketing OEM SDK; standard Bluetooth audio & EQ remain active."
        ),
        VendorBrandAdapter(
            brand = EarbudBrand.NOISE,
            nameKeywords = listOf("noise buds", "buds vs", "noise air", "colorfit"),
            companyIds = setOf(),
            vendorSdkNote = "Noise Buds proprietary GATT/RFCOMM protocol requires Nexxbase OEM SDK; standard Bluetooth audio & EQ remain active."
        )
    )

    private val standardAdapter = StandardBluetoothAdapter()

    fun identifyBrand(
        deviceName: String?,
        manufacturerId: Int? = null,
        manufacturerString: String? = null
    ): EarbudBrand {
        return adapters.firstOrNull {
            it.matches(deviceName, manufacturerId, manufacturerString)
        }?.brand ?: EarbudBrand.STANDARD
    }

    fun getAdapter(brand: EarbudBrand): BrandDeviceAdapter {
        return adapters.firstOrNull { it.brand == brand } ?: standardAdapter
    }

    fun allSupportedBrandDocs(): List<EarbudBrand> = EarbudBrand.entries
}

private class VendorBrandAdapter(
    override val brand: EarbudBrand,
    private val nameKeywords: List<String>,
    private val companyIds: Set<Int>,
    private val vendorSdkNote: String
) : BrandDeviceAdapter {

    override fun matches(
        deviceName: String?,
        manufacturerId: Int?,
        manufacturerString: String?
    ): Boolean {
        val haystack = listOfNotNull(deviceName, manufacturerString)
            .joinToString(" ")
            .lowercase()
        if (nameKeywords.any { haystack.contains(it) }) return true
        if (manufacturerId != null && companyIds.contains(manufacturerId)) return true
        return false
    }

    override fun buildCapabilities(
        hasGattBattery: Boolean,
        hasCombinedBattery: Boolean,
        hasFirmwareRevision: Boolean,
        hasImmediateAlert: Boolean,
        eqSupported: Boolean
    ): DeviceCapabilityMatrix {
        val map = linkedMapOf<EarbudFeatureId, FeatureCapability>()

        // Vendor-proprietary controls are honestly marked Pending Device Integration
        listOf(
            EarbudFeatureId.ANC_CONTROL,
            EarbudFeatureId.TRANSPARENCY_MODE,
            EarbudFeatureId.NORMAL_MODE,
            EarbudFeatureId.TOUCH_SINGLE_TAP,
            EarbudFeatureId.TOUCH_DOUBLE_TAP,
            EarbudFeatureId.TOUCH_TRIPLE_TAP,
            EarbudFeatureId.TOUCH_LONG_PRESS,
            EarbudFeatureId.IN_EAR_DETECTION,
            EarbudFeatureId.LOW_LATENCY_MODE,
            EarbudFeatureId.FIRMWARE_UPDATE
        ).forEach { feature ->
            map[feature] = FeatureCapability(
                feature = feature,
                status = CapabilityStatus.PENDING_INTEGRATION,
                detail = vendorSdkNote
            )
        }

        map[EarbudFeatureId.INDIVIDUAL_BATTERY] = FeatureCapability(
            feature = EarbudFeatureId.INDIVIDUAL_BATTERY,
            status = CapabilityStatus.PENDING_INTEGRATION,
            detail = "Separate L/R/Case battery telemetry requires ${brand.displayName} vendor protocol or Google Fast Pair metadata."
        )

        map[EarbudFeatureId.COMBINED_BATTERY] = FeatureCapability(
            feature = EarbudFeatureId.COMBINED_BATTERY,
            status = if (hasGattBattery || hasCombinedBattery) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = when {
                hasGattBattery -> "Read from standard BLE Battery Service (0x180F / 0x2A19)."
                hasCombinedBattery -> "Read from Android Bluetooth HFP/Platform battery metadata."
                else -> "Not exposed over standard HFP or BLE GATT Battery Service on this device."
            }
        )

        map[EarbudFeatureId.FIRMWARE_INFO] = FeatureCapability(
            feature = EarbudFeatureId.FIRMWARE_INFO,
            status = if (hasFirmwareRevision) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (hasFirmwareRevision) {
                "Read from BLE Device Information Service (0x180A / 0x2A26)."
            } else {
                "Firmware Revision characteristic (0x2A26) not exposed by this device over BLE GATT."
            }
        )

        map[EarbudFeatureId.BLE_IMMEDIATE_ALERT] = FeatureCapability(
            feature = EarbudFeatureId.BLE_IMMEDIATE_ALERT,
            status = if (hasImmediateAlert) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (hasImmediateAlert) {
                "BLE Immediate Alert Service (0x1802) detected."
            } else {
                "Device does not advertise BLE Immediate Alert (0x1802); audio tone alert via A2DP is used instead."
            }
        )

        map[EarbudFeatureId.AUDIO_FX_EQ] = FeatureCapability(
            feature = EarbudFeatureId.AUDIO_FX_EQ,
            status = if (eqSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (eqSupported) {
                "Powered by Android hardware AudioFX Equalizer, BassBoost & Virtualizer."
            } else {
                "Android AudioFX Equalizer unavailable on current output session."
            }
        )

        return DeviceCapabilityMatrix(brand = brand, capabilities = map)
    }
}

private class StandardBluetoothAdapter : BrandDeviceAdapter {
    override val brand: EarbudBrand = EarbudBrand.STANDARD

    override fun matches(
        deviceName: String?,
        manufacturerId: Int?,
        manufacturerString: String?
    ): Boolean = true

    override fun buildCapabilities(
        hasGattBattery: Boolean,
        hasCombinedBattery: Boolean,
        hasFirmwareRevision: Boolean,
        hasImmediateAlert: Boolean,
        eqSupported: Boolean
    ): DeviceCapabilityMatrix {
        val map = linkedMapOf<EarbudFeatureId, FeatureCapability>()

        listOf(
            EarbudFeatureId.ANC_CONTROL,
            EarbudFeatureId.TRANSPARENCY_MODE,
            EarbudFeatureId.NORMAL_MODE,
            EarbudFeatureId.TOUCH_SINGLE_TAP,
            EarbudFeatureId.TOUCH_DOUBLE_TAP,
            EarbudFeatureId.TOUCH_TRIPLE_TAP,
            EarbudFeatureId.TOUCH_LONG_PRESS,
            EarbudFeatureId.IN_EAR_DETECTION,
            EarbudFeatureId.LOW_LATENCY_MODE,
            EarbudFeatureId.FIRMWARE_UPDATE,
            EarbudFeatureId.INDIVIDUAL_BATTERY
        ).forEach { feature ->
            map[feature] = FeatureCapability(
                feature = feature,
                status = CapabilityStatus.UNSUPPORTED,
                detail = "Not Supported on This Device — standard Bluetooth A2DP/HFP does not expose vendor hardware controls."
            )
        }

        map[EarbudFeatureId.COMBINED_BATTERY] = FeatureCapability(
            feature = EarbudFeatureId.COMBINED_BATTERY,
            status = if (hasGattBattery || hasCombinedBattery) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (hasGattBattery || hasCombinedBattery) {
                "Read via standard Bluetooth battery reporting."
            } else {
                "Device did not report battery over HFP or BLE GATT (0x180F)."
            }
        )

        map[EarbudFeatureId.FIRMWARE_INFO] = FeatureCapability(
            feature = EarbudFeatureId.FIRMWARE_INFO,
            status = if (hasFirmwareRevision) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (hasFirmwareRevision) {
                "Read via BLE Device Information Service (0x180A)."
            } else {
                "Not Supported on This Device — BLE GATT 0x2A26 unavailable."
            }
        )

        map[EarbudFeatureId.BLE_IMMEDIATE_ALERT] = FeatureCapability(
            feature = EarbudFeatureId.BLE_IMMEDIATE_ALERT,
            status = if (hasImmediateAlert) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (hasImmediateAlert) {
                "BLE Immediate Alert Service (0x1802) active."
            } else {
                "BLE Immediate Alert Service (0x1802) not present; audio chime alert is supported over A2DP."
            }
        )

        map[EarbudFeatureId.AUDIO_FX_EQ] = FeatureCapability(
            feature = EarbudFeatureId.AUDIO_FX_EQ,
            status = if (eqSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            detail = if (eqSupported) {
                "Android AudioFX Equalizer, BassBoost & Virtualizer active."
            } else {
                "Hardware AudioFX session restricted on this device."
            }
        )

        return DeviceCapabilityMatrix(brand = brand, capabilities = map)
    }
}
