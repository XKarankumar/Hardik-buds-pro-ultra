package com.example.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.model.ConnectedEarbudState
import com.example.domain.model.DiscoveredBluetoothDevice
import com.example.domain.model.EarbudBrand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed interface BluetoothEvent {
    data class DeviceConnected(val device: ConnectedEarbudState) : BluetoothEvent
    data class DeviceDisconnected(val name: String, val address: String, val unexpected: Boolean) : BluetoothEvent
    data class BatteryUpdated(val name: String, val address: String, val percent: Int) : BluetoothEvent
    data class RadioStateChanged(val enabled: Boolean) : BluetoothEvent
    data class PairingFailed(val deviceName: String, val reason: String) : BluetoothEvent
    data class ErrorMessage(val message: String) : BluetoothEvent
}

class BluetoothHardwareManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothHardwareMgr"
        private const val SCAN_TIMEOUT_MS = 15_000L
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val DEVICE_INFO_SERVICE_UUID: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val FIRMWARE_REV_CHAR_UUID: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        val MANUFACTURER_CHAR_UUID: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val MODEL_NUMBER_CHAR_UUID: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
        val IMMEDIATE_ALERT_SERVICE_UUID: UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb")
        val ALERT_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var activeGatt: BluetoothGatt? = null
    private var eqSupportedFlag: Boolean = true
    private var isReceiverRegistered = false
    private var activeScanner: BluetoothLeScanner? = null

    private val _isHardwareSupported = MutableStateFlow(bluetoothAdapter != null)
    val isHardwareSupported: StateFlow<Boolean> = _isHardwareSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val bondedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _bondedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedEarbud = MutableStateFlow<ConnectedEarbudState?>(null)
    val connectedEarbud: StateFlow<ConnectedEarbudState?> = _connectedEarbud.asStateFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()

    private val discoveredMap = LinkedHashMap<String, DiscoveredBluetoothDevice>()
    private val rssiMap = HashMap<String, Int>()
    private var gattReadQueue: ArrayDeque<BluetoothGattCharacteristic> = ArrayDeque()
    private var lastGattBattery: Int? = null
    private var lastFirmwareRev: String? = null
    private var lastManufacturerName: String? = null
    private var lastModelNumber: String? = null
    private var hasImmediateAlert: Boolean = false
    private var wasExplicitDisconnect: Boolean = false

    private val scanTimeoutRunnable = Runnable {
        stopScan()
        _scanStatusMessage.value = "Scan completed (15s timeout)."
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProxy = proxy as? BluetoothA2dp
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProxy = proxy as? BluetoothHeadset
            }
            refreshAllStates()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = null
            if (profile == BluetoothProfile.HEADSET) headsetProxy = null
            refreshAllStates()
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasScanPermission()) return
            val device = result.device ?: return
            val address = device.address ?: return
            val rssi = result.rssi
            rssiMap[address] = rssi

            val deviceName = safeDeviceName(device) ?: result.scanRecord?.deviceName ?: "BLE Device ($address)"
            val mfgData = result.scanRecord?.manufacturerSpecificData
            val manufacturerId = if (mfgData != null && mfgData.size() > 0) mfgData.keyAt(0) else null
            val brand = BrandIntegrationRegistry.identifyBrand(deviceName, manufacturerId, null)

            val existing = discoveredMap[address]
            val isAudio = existing?.isAudioLikely == true || isLikelyAudioDevice(device) || brand != EarbudBrand.STANDARD

            discoveredMap[address] = DiscoveredBluetoothDevice(
                name = deviceName,
                address = address,
                rssiDbm = rssi,
                isBonded = safeBondState(device) == BluetoothDevice.BOND_BONDED,
                isConnected = _connectedEarbud.value?.address == address,
                isBle = true,
                isClassic = existing?.isClassic == true,
                isAudioLikely = isAudio,
                brand = brand
            )
            _discoveredDevices.value = discoveredMap.values.sortedByDescending { it.rssiDbm ?: -120 }

            if (_connectedEarbud.value?.address == address) {
                _connectedEarbud.value = _connectedEarbud.value?.copy(rssiDbm = rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            val msg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "BLE scan already active."
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "BLE scanner registration failed on chipset."
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning unsupported on this device."
                else -> "BLE scan failed (code $errorCode)."
            }
            _scanStatusMessage.value = msg
            _events.tryEmit(BluetoothEvent.ErrorMessage(msg))
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    _isBluetoothEnabled.value = enabled
                    _events.tryEmit(BluetoothEvent.RadioStateChanged(enabled))
                    if (!enabled) {
                        stopScan()
                        closeGatt()
                        _connectedEarbud.value = null
                    } else {
                        refreshAllStates()
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasScanPermission()) return
                    val device = extractDevice(intent) ?: return
                    val address = device.address ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).let {
                        if (it == Short.MIN_VALUE) null else it.toInt()
                    }
                    if (rssi != null) rssiMap[address] = rssi
                    val name = safeDeviceName(device) ?: "Bluetooth Device ($address)"
                    val brand = BrandIntegrationRegistry.identifyBrand(name)
                    val existing = discoveredMap[address]
                    discoveredMap[address] = DiscoveredBluetoothDevice(
                        name = name,
                        address = address,
                        rssiDbm = rssi ?: existing?.rssiDbm,
                        isBonded = safeBondState(device) == BluetoothDevice.BOND_BONDED,
                        isConnected = _connectedEarbud.value?.address == address,
                        isBle = existing?.isBle == true,
                        isClassic = true,
                        isAudioLikely = isLikelyAudioDevice(device) || brand != EarbudBrand.STANDARD,
                        brand = brand
                    )
                    _discoveredDevices.value = discoveredMap.values.sortedByDescending { it.rssiDbm ?: -120 }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (_isScanning.value) {
                        _scanStatusMessage.value = "Discovery sweep finished."
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    if (!hasConnectPermission()) return
                    val device = extractDevice(intent) ?: return
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
                    val name = safeDeviceName(device) ?: device.address ?: "Earbuds"
                    if (prevState == BluetoothDevice.BOND_BONDING && bondState == BluetoothDevice.BOND_NONE) {
                        _events.tryEmit(BluetoothEvent.PairingFailed(name, "Pairing rejected or timed out."))
                    }
                    refreshAllStates()
                }

                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val prev = _connectedEarbud.value
                    refreshAllStates()
                    val now = _connectedEarbud.value
                    if (prev == null && now != null && now.isConnected) {
                        wasExplicitDisconnect = false
                        _events.tryEmit(BluetoothEvent.DeviceConnected(now))
                    } else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED && prev != null) {
                        val unexpected = !wasExplicitDisconnect
                        wasExplicitDisconnect = false
                        _events.tryEmit(BluetoothEvent.DeviceDisconnected(prev.deviceName, prev.address, unexpected))
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val prev = _connectedEarbud.value
                    closeGatt()
                    refreshAllStates()
                    if (prev != null && _connectedEarbud.value == null) {
                        val unexpected = !wasExplicitDisconnect
                        wasExplicitDisconnect = false
                        _events.tryEmit(BluetoothEvent.DeviceDisconnected(prev.deviceName, prev.address, unexpected))
                    }
                }

                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = extractDevice(intent) ?: return
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (level in 0..100) {
                        refreshAllStates()
                        val name = safeDeviceName(device) ?: device.address ?: "Earbuds"
                        _events.tryEmit(BluetoothEvent.BatteryUpdated(name, device.address, level))
                    }
                }
            }
        }
    }

    fun startMonitoring() {
        val adapter = bluetoothAdapter
        _isHardwareSupported.value = adapter != null
        _isBluetoothEnabled.value = adapter?.isEnabled == true

        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(ACTION_BATTERY_LEVEL_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
            isReceiverRegistered = true
        }

        if (adapter != null) {
            try {
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            } catch (e: Exception) {
                Log.w(TAG, "Profile proxy init warning: ${e.message}")
            }
        }
        refreshAllStates()
    }

    fun stopMonitoring() {
        stopScan()
        closeGatt()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
            } catch (_: IllegalArgumentException) {
            }
            isReceiverRegistered = false
        }
        try {
            a2dpProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
            headsetProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
        } catch (_: Exception) {
        }
        a2dpProxy = null
        headsetProxy = null
    }

    fun updateAudioEffectSupport(eqSupported: Boolean) {
        eqSupportedFlag = eqSupported
        _connectedEarbud.value?.let { current ->
            val adapter = BrandIntegrationRegistry.getAdapter(current.brand)
            val updatedMatrix = adapter.buildCapabilities(
                hasGattBattery = lastGattBattery != null,
                hasCombinedBattery = current.combinedBatteryPercent != null,
                hasFirmwareRevision = current.firmwareRevision != null,
                hasImmediateAlert = current.hasImmediateAlertService,
                eqSupported = eqSupported
            )
            _connectedEarbud.value = current.copy(capabilityMatrix = updatedMatrix)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _scanStatusMessage.value = "Bluetooth hardware unavailable."
            return
        }
        if (!adapter.isEnabled) {
            _scanStatusMessage.value = "Enable Bluetooth first to scan."
            return
        }
        if (!hasScanPermission()) {
            _scanStatusMessage.value = "Nearby Devices / Scan permission required."
            return
        }

        discoveredMap.clear()
        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        _scanStatusMessage.value = "Scanning BLE & Classic audio devices..."

        try {
            activeScanner = adapter.bluetoothLeScanner
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            activeScanner?.startScan(null, settings, bleScanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "BLE startScan fallback: ${e.message}")
        }

        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "Classic discovery fallback: ${e.message}")
        }

        mainHandler.removeCallbacks(scanTimeoutRunnable)
        mainHandler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        if (!_isScanning.value) return
        _isScanning.value = false
        if (hasScanPermission()) {
            try {
                activeScanner?.stopScan(bleScanCallback)
            } catch (_: Exception) {
            }
            try {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter.cancelDiscovery()
                }
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(address: String): Boolean {
        if (!hasConnectPermission()) {
            _events.tryEmit(BluetoothEvent.ErrorMessage("Bluetooth Connect permission required to pair."))
            return false
        }
        val device = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            null
        } ?: return false

        return try {
            stopScan()
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
            } else {
                connectToDevice(address)
                true
            }
        } catch (e: Exception) {
            _events.tryEmit(BluetoothEvent.PairingFailed(safeDeviceName(device) ?: address, e.message ?: "Bond error"))
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        if (!hasConnectPermission()) {
            _events.tryEmit(BluetoothEvent.ErrorMessage("Bluetooth Connect permission required."))
            return
        }
        val device = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            null
        } ?: return

        wasExplicitDisconnect = false
        // Attempt GATT connection for standard Battery (0x180F), Device Info (0x180A), Immediate Alert (0x1802)
        connectGatt(device)

        // Attempt A2DP / Headset profile connect via Android BluetoothProfile connect helper if available, else guide to Settings
        var attemptedProfileConnect = false
        a2dpProxy?.let { proxy ->
            try {
                val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                method.invoke(proxy, device)
                attemptedProfileConnect = true
            } catch (_: Exception) {
            }
        }
        headsetProxy?.let { proxy ->
            try {
                val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                method.invoke(proxy, device)
                attemptedProfileConnect = true
            } catch (_: Exception) {
            }
        }
        if (!attemptedProfileConnect) {
            _scanStatusMessage.value = "GATT requested; finish A2DP audio routing in System Bluetooth Settings if needed."
        }
        refreshAllStates()
    }

    @SuppressLint("MissingPermission")
    fun disconnectCurrentDevice() {
        val current = _connectedEarbud.value ?: return
        wasExplicitDisconnect = true
        closeGatt()

        val device = try {
            bluetoothAdapter?.getRemoteDevice(current.address)
        } catch (_: Exception) {
            null
        }
        var disconnectedViaProxy = false
        if (device != null) {
            a2dpProxy?.let { proxy ->
                try {
                    val m = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                    m.invoke(proxy, device)
                    disconnectedViaProxy = true
                } catch (_: Exception) {
                }
            }
            headsetProxy?.let { proxy ->
                try {
                    val m = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                    m.invoke(proxy, device)
                    disconnectedViaProxy = true
                } catch (_: Exception) {
                }
            }
        }
        if (!disconnectedViaProxy) {
            openSystemBluetoothSettings()
        }
        refreshAllStates()
    }

    @SuppressLint("MissingPermission")
    fun triggerBleImmediateAlert(highAlert: Boolean): Boolean {
        val gatt = activeGatt ?: return false
        if (!hasConnectPermission()) return false
        val service = gatt.getService(IMMEDIATE_ALERT_SERVICE_UUID) ?: return false
        val char = service.getCharacteristic(ALERT_LEVEL_CHAR_UUID) ?: return false
        val levelByte = byteArrayOf(if (highAlert) 0x02 else 0x00)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, levelByte, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) == 0
            } else {
                @Suppress("DEPRECATION")
                char.value = levelByte
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openSystemBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun refreshAllStates() {
        val adapter = bluetoothAdapter
        _isHardwareSupported.value = adapter != null
        _isBluetoothEnabled.value = adapter?.isEnabled == true

        if (adapter == null || !adapter.isEnabled || !hasConnectPermission()) {
            _bondedDevices.value = emptyList()
            _connectedEarbud.value = null
            return
        }

        val bonded = try {
            adapter.bondedDevices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        val a2dpConnected = try {
            a2dpProxy?.connectedDevices.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        val hfpConnected = try {
            headsetProxy?.connectedDevices.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        val connectedAddressSet = (a2dpConnected + hfpConnected).map { it.address }.toSet()

        _bondedDevices.value = bonded.map { dev ->
            val name = safeDeviceName(dev) ?: dev.address ?: "Unknown Device"
            val brand = BrandIntegrationRegistry.identifyBrand(name)
            DiscoveredBluetoothDevice(
                name = name,
                address = dev.address,
                rssiDbm = rssiMap[dev.address],
                isBonded = true,
                isConnected = connectedAddressSet.contains(dev.address) || activeGatt?.device?.address == dev.address,
                isBle = dev.type == BluetoothDevice.DEVICE_TYPE_LE || dev.type == BluetoothDevice.DEVICE_TYPE_DUAL,
                isClassic = dev.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || dev.type == BluetoothDevice.DEVICE_TYPE_DUAL,
                isAudioLikely = isLikelyAudioDevice(dev) || brand != EarbudBrand.STANDARD,
                brand = brand
            )
        }.sortedWith(compareByDescending<DiscoveredBluetoothDevice> { it.isConnected }.thenByDescending { it.isAudioLikely })

        // Pick primary connected audio device (prefer A2DP, then HFP, then GATT)
        val primaryDevice = a2dpConnected.firstOrNull()
            ?: hfpConnected.firstOrNull()
            ?: activeGatt?.device

        if (primaryDevice == null) {
            _connectedEarbud.value = null
            return
        }

        val address = primaryDevice.address
        val name = safeDeviceName(primaryDevice) ?: address
        val brand = BrandIntegrationRegistry.identifyBrand(
            deviceName = name,
            manufacturerString = lastManufacturerName
        )
        val platformBattery = readPlatformBatteryLevel(primaryDevice)
        val combinedBattery = lastGattBattery ?: platformBattery
        val batterySource = when {
            lastGattBattery != null -> "BLE GATT Battery Service (0x180F)"
            platformBattery != null -> "Android Bluetooth HFP / Platform Metadata"
            else -> "Not exposed by earbuds over standard Bluetooth"
        }

        val isA2dp = a2dpConnected.any { it.address == address }
        val isHfp = hfpConnected.any { it.address == address }
        val isGatt = activeGatt?.device?.address == address

        val matrix = BrandIntegrationRegistry.getAdapter(brand).buildCapabilities(
            hasGattBattery = lastGattBattery != null,
            hasCombinedBattery = platformBattery != null,
            hasFirmwareRevision = lastFirmwareRev != null,
            hasImmediateAlert = hasImmediateAlert,
            eqSupported = eqSupportedFlag
        )

        _connectedEarbud.value = ConnectedEarbudState(
            deviceName = name,
            address = address,
            brand = brand,
            isConnected = isA2dp || isHfp || isGatt,
            a2dpConnected = isA2dp,
            hfpConnected = isHfp,
            gattConnected = isGatt,
            combinedBatteryPercent = combinedBattery,
            leftBatteryPercent = null, // Never fabricated; only populated when vendor SDK/FastPair protocol exposes it
            rightBatteryPercent = null,
            caseBatteryPercent = null,
            isCharging = null,
            batterySourceNote = batterySource,
            firmwareRevision = lastFirmwareRev,
            manufacturerName = lastManufacturerName,
            modelNumber = lastModelNumber,
            rssiDbm = rssiMap[address],
            hasImmediateAlertService = hasImmediateAlert,
            capabilityMatrix = matrix
        )

        if (!isGatt && (primaryDevice.type == BluetoothDevice.DEVICE_TYPE_LE || primaryDevice.type == BluetoothDevice.DEVICE_TYPE_DUAL)) {
            connectGatt(primaryDevice)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectGatt(device: BluetoothDevice) {
        if (!hasConnectPermission()) return
        if (activeGatt?.device?.address == device.address) return
        closeGatt()

        activeGatt = device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                        gatt.readRemoteRssi()
                        mainHandler.post { refreshAllStates() }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mainHandler.post {
                            closeGatt()
                            refreshAllStates()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) return
                    hasImmediateAlert = gatt.getService(IMMEDIATE_ALERT_SERVICE_UUID) != null
                    gattReadQueue.clear()

                    gatt.getService(BATTERY_SERVICE_UUID)?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)?.let {
                        gattReadQueue.add(it)
                    }
                    gatt.getService(DEVICE_INFO_SERVICE_UUID)?.let { info ->
                        info.getCharacteristic(FIRMWARE_REV_CHAR_UUID)?.let { gattReadQueue.add(it) }
                        info.getCharacteristic(MANUFACTURER_CHAR_UUID)?.let { gattReadQueue.add(it) }
                        info.getCharacteristic(MODEL_NUMBER_CHAR_UUID)?.let { gattReadQueue.add(it) }
                    }
                    readNextGattCharacteristic(gatt)
                    mainHandler.post { refreshAllStates() }
                }

                @Deprecated("Deprecated in API 33")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    @Suppress("DEPRECATION")
                    handleGattValue(characteristic.uuid, characteristic.value, status)
                    readNextGattCharacteristic(gatt)
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
                ) {
                    handleGattValue(characteristic.uuid, value, status)
                    readNextGattCharacteristic(gatt)
                }

                override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        rssiMap[gatt.device.address] = rssi
                        mainHandler.post { refreshAllStates() }
                    }
                }
            },
            BluetoothDevice.TRANSPORT_LE
        )
    }

    @SuppressLint("MissingPermission")
    private fun readNextGattCharacteristic(gatt: BluetoothGatt) {
        if (!hasConnectPermission()) return
        val next = gattReadQueue.removeFirstOrNull() ?: return
        try {
            gatt.readCharacteristic(next)
        } catch (_: Exception) {
        }
    }

    private fun handleGattValue(uuid: UUID, value: ByteArray?, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS || value == null || value.isEmpty()) return
        when (uuid) {
            BATTERY_LEVEL_CHAR_UUID -> {
                val level = value[0].toInt() and 0xFF
                if (level in 0..100) {
                    lastGattBattery = level
                }
            }
            FIRMWARE_REV_CHAR_UUID -> {
                lastFirmwareRev = value.decodeToString().trim().ifEmpty { null }
            }
            MANUFACTURER_CHAR_UUID -> {
                lastManufacturerName = value.decodeToString().trim().ifEmpty { null }
            }
            MODEL_NUMBER_CHAR_UUID -> {
                lastModelNumber = value.decodeToString().trim().ifEmpty { null }
            }
        }
        mainHandler.post { refreshAllStates() }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {
        }
        activeGatt = null
        lastGattBattery = null
        lastFirmwareRev = null
        lastManufacturerName = null
        lastModelNumber = null
        hasImmediateAlert = false
        gattReadQueue.clear()
    }

    private fun readPlatformBatteryLevel(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else null
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return if (hasConnectPermission()) {
            try {
                device.name?.takeIf { it.isNotBlank() }
            } catch (_: SecurityException) {
                null
            }
        } else null
    }

    @SuppressLint("MissingPermission")
    private fun safeBondState(device: BluetoothDevice): Int {
        return if (hasConnectPermission()) {
            try {
                device.bondState
            } catch (_: SecurityException) {
                BluetoothDevice.BOND_NONE
            }
        } else BluetoothDevice.BOND_NONE
    }

    @SuppressLint("MissingPermission")
    private fun isLikelyAudioDevice(device: BluetoothDevice): Boolean {
        if (!hasConnectPermission()) return false
        return try {
            val major = device.bluetoothClass?.majorDeviceClass
            major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                major == BluetoothClass.Device.Major.WEARABLE
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        }
    }
}
