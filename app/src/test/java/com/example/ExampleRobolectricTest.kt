package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.bluetooth.BrandIntegrationRegistry
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.BudsLocalRepository
import com.example.data.local.ConnectionEventEntity
import com.example.data.local.HardikBudsDatabase
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.security.KeystoreSecurityManager
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.EarbudBrand
import com.example.domain.model.EarbudFeatureId
import com.example.domain.model.EqPreset
import com.example.domain.model.SoundMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: HardikBudsDatabase
    private lateinit var repo: BudsLocalRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HardikBudsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = BudsLocalRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name and brand resources match HARDIK BUDS PRO ULTRA`() {
        assertEquals("Hardik Buds Pro", context.getString(R.string.app_name))
        assertEquals("HARDIK BUDS PRO ULTRA", context.getString(R.string.brand_full_title))
    }

    @Test
    fun `brand registry identifies all 11 earbud brands and marks proprietary controls honestly`() {
        assertEquals(EarbudBrand.ONEPLUS, BrandIntegrationRegistry.identifyBrand("OnePlus Buds Pro 3"))
        assertEquals(EarbudBrand.SAMSUNG, BrandIntegrationRegistry.identifyBrand("Galaxy Buds2 Pro"))
        assertEquals(EarbudBrand.APPLE, BrandIntegrationRegistry.identifyBrand("AirPods Pro", manufacturerId = 0x004C))
        assertEquals(EarbudBrand.REALME, BrandIntegrationRegistry.identifyBrand("realme Buds Air 6"))
        assertEquals(EarbudBrand.OPPO, BrandIntegrationRegistry.identifyBrand("OPPO Enco Air3"))
        assertEquals(EarbudBrand.XIAOMI, BrandIntegrationRegistry.identifyBrand("Redmi Buds 5 Pro"))
        assertEquals(EarbudBrand.JBL, BrandIntegrationRegistry.identifyBrand("JBL Tune Beam"))
        assertEquals(EarbudBrand.SONY, BrandIntegrationRegistry.identifyBrand("WF-1000XM5"))
        assertEquals(EarbudBrand.BOAT, BrandIntegrationRegistry.identifyBrand("boAt Airdopes 141"))
        assertEquals(EarbudBrand.NOISE, BrandIntegrationRegistry.identifyBrand("Noise Buds VS104"))
        assertEquals(EarbudBrand.STANDARD, BrandIntegrationRegistry.identifyBrand("Generic TWS Audio"))

        val sonyMatrix = BrandIntegrationRegistry.getAdapter(EarbudBrand.SONY).buildCapabilities(
            hasGattBattery = true,
            hasCombinedBattery = true,
            hasFirmwareRevision = true,
            hasImmediateAlert = false,
            eqSupported = true
        )
        assertEquals(CapabilityStatus.SUPPORTED, sonyMatrix.getStatus(EarbudFeatureId.COMBINED_BATTERY))
        assertEquals(CapabilityStatus.SUPPORTED, sonyMatrix.getStatus(EarbudFeatureId.FIRMWARE_INFO))
        assertEquals(CapabilityStatus.SUPPORTED, sonyMatrix.getStatus(EarbudFeatureId.AUDIO_FX_EQ))
        assertEquals(CapabilityStatus.PENDING_INTEGRATION, sonyMatrix.getStatus(EarbudFeatureId.ANC_CONTROL))
        assertEquals(CapabilityStatus.UNSUPPORTED, sonyMatrix.getStatus(EarbudFeatureId.BLE_IMMEDIATE_ALERT))

        val genericMatrix = BrandIntegrationRegistry.getAdapter(EarbudBrand.STANDARD).buildCapabilities(
            hasGattBattery = false,
            hasCombinedBattery = false,
            hasFirmwareRevision = false,
            hasImmediateAlert = false,
            eqSupported = true
        )
        assertEquals(CapabilityStatus.UNSUPPORTED, genericMatrix.getStatus(EarbudFeatureId.ANC_CONTROL))
        assertEquals(CapabilityStatus.UNSUPPORTED, genericMatrix.getStatus(EarbudFeatureId.COMBINED_BATTERY))
    }

    @Test
    fun `security manager enforces 4-6 digit pin, rate limit lockout, and recovery`() {
        val sec = KeystoreSecurityManager(context)
        assertFalse(sec.configurePin("123", "alpha")) // Invalid length
        assertTrue(sec.configurePin("2580", "obsidian phrase"))
        assertTrue(sec.state.value.isPinConfigured)
        assertEquals(4, sec.state.value.pinLength)

        assertTrue(sec.verifyPin("2580") is KeystoreSecurityManager.PinVerifyResult.Success)

        // Trigger 5 failed attempts -> lockout
        repeat(4) {
            val res = sec.verifyPin("0000", nowMillis = 10_000L)
            assertTrue(res is KeystoreSecurityManager.PinVerifyResult.Invalid)
        }
        val fifth = sec.verifyPin("0000", nowMillis = 10_000L)
        assertTrue(fifth is KeystoreSecurityManager.PinVerifyResult.LockedOut)

        // Recovery resets PIN
        assertFalse(sec.recoverWithPhrase("wrong phrase", "135790"))
        assertTrue(sec.recoverWithPhrase("obsidian phrase", "135790"))
        assertEquals(6, sec.state.value.pinLength)
        assertTrue(sec.verifyPin("135790") is KeystoreSecurityManager.PinVerifyResult.Success)
    }

    @Test
    fun `room repository persists battery history, connection events, owner devices, and custom EQ presets`() = runTest {
        repo.recordBatterySample(
            BatteryHistoryEntity(
                deviceAddress = "AA:BB:CC:DD:EE:FF",
                deviceName = "Sony WF-1000XM5",
                combinedPercent = 82,
                leftPercent = null,
                rightPercent = null,
                casePercent = null,
                isCharging = false,
                sourceLabel = "BLE GATT 0x180F"
            )
        )
        repo.recordConnectionEvent(
            ConnectionEventEntity(
                deviceAddress = "AA:BB:CC:DD:EE:FF",
                deviceName = "Sony WF-1000XM5",
                brandName = "Sony",
                eventType = "CONNECTED",
                latitude = 28.6139,
                longitude = 77.2090,
                locationAccuracyMeters = 18f,
                details = "A2DP + GATT"
            )
        )
        repo.registerOwnerDevice(
            RegisteredDeviceEntity(
                address = "AA:BB:CC:DD:EE:FF",
                deviceName = "Sony WF-1000XM5",
                brandName = "Sony",
                customAlias = "My Daily Buds"
            )
        )
        repo.saveCustomEqPreset(
            EqPreset(
                id = "custom_test",
                name = "Warm Studio",
                bands10Db = listOf(4, 3, 2, 1, 0, 0, 1, 2, 2, 1),
                bassBoostStrength = 400,
                virtualizerStrength = 300,
                soundMode = SoundMode.CUSTOM,
                isCustom = true
            )
        )

        val batteries = repo.batteryHistory.first()
        val events = repo.connectionEvents.first()
        val owners = repo.registeredDevices.first()
        val presets = repo.customEqPresets.first()

        assertEquals(1, batteries.size)
        assertEquals(82, batteries[0].combinedPercent)
        assertEquals(1, events.size)
        assertNotNull(events[0].latitude)
        assertEquals(1, owners.size)
        assertEquals("My Daily Buds", owners[0].customAlias)
        assertEquals(1, presets.size)
        assertEquals("Warm Studio", presets[0].name)
    }
}
