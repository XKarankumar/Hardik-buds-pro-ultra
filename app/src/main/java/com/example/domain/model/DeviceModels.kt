package com.example.domain.model

enum class EarbudBrand(
    val displayName: String,
    val protocolSummary: String
) {
    ONEPLUS("OnePlus Buds", "Standard A2DP/HFP/GATT + HeyMelody Vendor SDK Pending"),
    SAMSUNG("Samsung Galaxy Buds", "Standard A2DP/HFP/GATT + Galaxy Wearable SDK Pending"),
    APPLE("Apple AirPods", "Standard A2DP/HFP Audio + Apple AACP Proprietary Protocol"),
    REALME("Realme Buds", "Standard A2DP/HFP/GATT + Realme Link Protocol Pending"),
    OPPO("Oppo Enco", "Standard A2DP/HFP/GATT + HeyMelody Protocol Pending"),
    XIAOMI("Xiaomi Redmi Buds", "Standard A2DP/HFP/GATT + Mi Earbuds Protocol Pending"),
    JBL("JBL", "Standard A2DP/HFP/GATT + Harman/JBL SDK Pending"),
    SONY("Sony", "Standard A2DP/HFP/GATT + Sony Headphones Connect Protocol Pending"),
    BOAT("boAt", "Standard A2DP/HFP/GATT + boAt Hearables Protocol Pending"),
    NOISE("Noise", "Standard A2DP/HFP/GATT + NoiseFit Audio Protocol Pending"),
    STANDARD("Standard Bluetooth Audio", "Bluetooth SIG Classic A2DP / HFP / BLE GATT")
}

enum class CapabilityStatus(val badgeText: String) {
    SUPPORTED("Supported"),
    UNSUPPORTED("Not Supported on This Device"),
    PENDING_INTEGRATION("Pending Device Integration")
}

enum class EarbudFeatureId(val title: String, val category: String) {
    ANC_CONTROL("Active Noise Cancellation (ANC)", "Noise Control"),
    TRANSPARENCY_MODE("Transparency / Ambient Mode", "Noise Control"),
    NORMAL_MODE("Normal Acoustic Mode", "Noise Control"),
    TOUCH_SINGLE_TAP("Single Tap Action", "Touch Controls"),
    TOUCH_DOUBLE_TAP("Double Tap Action", "Touch Controls"),
    TOUCH_TRIPLE_TAP("Triple Tap Action", "Touch Controls"),
    TOUCH_LONG_PRESS("Long Press Action", "Touch Controls"),
    IN_EAR_DETECTION("In-Ear Wear Detection", "Sensors"),
    LOW_LATENCY_MODE("Hardware Low-Latency Mode", "Performance"),
    FIRMWARE_INFO("Firmware Version Readout", "System"),
    FIRMWARE_UPDATE("Firmware OTA Update", "System"),
    INDIVIDUAL_BATTERY("Left / Right / Case Battery", "Telemetry"),
    COMBINED_BATTERY("Standard Headset / GATT Battery", "Telemetry"),
    BLE_IMMEDIATE_ALERT("BLE Immediate Alert (0x1802)", "Find My"),
    AUDIO_FX_EQ("Android Hardware 10-Band EQ", "Audio Studio")
}

data class FeatureCapability(
    val feature: EarbudFeatureId,
    val status: CapabilityStatus,
    val detail: String
)

data class DeviceCapabilityMatrix(
    val brand: EarbudBrand,
    val capabilities: Map<EarbudFeatureId, FeatureCapability>
) {
    fun getStatus(feature: EarbudFeatureId): CapabilityStatus =
        capabilities[feature]?.status ?: CapabilityStatus.UNSUPPORTED

    fun getDetail(feature: EarbudFeatureId): String =
        capabilities[feature]?.detail ?: "Not supported on this device."
}

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val rssiDbm: Int?,
    val isBonded: Boolean,
    val isConnected: Boolean,
    val isBle: Boolean,
    val isClassic: Boolean,
    val isAudioLikely: Boolean,
    val brand: EarbudBrand,
    val lastSeenMillis: Long = System.currentTimeMillis()
)

data class ConnectedEarbudState(
    val deviceName: String,
    val address: String,
    val brand: EarbudBrand,
    val isConnected: Boolean,
    val a2dpConnected: Boolean,
    val hfpConnected: Boolean,
    val gattConnected: Boolean,
    val combinedBatteryPercent: Int?,
    val leftBatteryPercent: Int?,
    val rightBatteryPercent: Int?,
    val caseBatteryPercent: Int?,
    val isCharging: Boolean?,
    val batterySourceNote: String,
    val firmwareRevision: String?,
    val manufacturerName: String?,
    val modelNumber: String?,
    val rssiDbm: Int?,
    val hasImmediateAlertService: Boolean,
    val capabilityMatrix: DeviceCapabilityMatrix
)
