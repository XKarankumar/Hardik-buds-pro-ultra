package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AtmosphericBackdrop
import com.example.ui.components.DynamicIslandPill
import com.example.ui.components.rememberAppHaptic
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.AnimatedSplashOverlay
import com.example.ui.screens.AudioStudioScreen
import com.example.ui.screens.ConnectionPopupModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceCompatibilityScreen
import com.example.ui.screens.FindMyScreen
import com.example.ui.screens.PinLockOverlay
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartControlsScreen
import com.example.ui.theme.HardikBudsProTheme
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.viewmodel.BudsCompanionViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private data class NavTab(
    val route: AppRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : FragmentActivity() {

    private var appliedLangCode: String = "en"

    override fun attachBaseContext(newBase: Context) {
        val langCode = newBase.getSharedPreferences("hardik_buds_locale", Context.MODE_PRIVATE)
            .getString("lang_code", "en") ?: "en"
        appliedLangCode = langCode
        val locale = Locale.forLanguageTag(langCode)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: BudsCompanionViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val securityState by viewModel.securityState.collectAsStateWithLifecycle()
            val pinFeedback by viewModel.pinFeedback.collectAsStateWithLifecycle()
            val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(900L)
                showSplash = false
            }

            LaunchedEffect(prefs.language.code) {
                if (prefs.language.code != appliedLangCode) {
                    getSharedPreferences("hardik_buds_locale", Context.MODE_PRIVATE)
                        .edit()
                        .putString("lang_code", prefs.language.code)
                        .apply()
                    recreate()
                }
            }

            HardikBudsProTheme(
                themeStyle = prefs.themeStyle,
                themeMode = prefs.themeMode,
                accentPalette = prefs.accentPalette
            ) {
                val onHaptic = rememberAppHaptic(prefs.hapticsEnabled)

                Box(modifier = Modifier.fillMaxSize()) {
                    if (securityState.isAppLocked && !showSplash) {
                        PinLockOverlay(
                            securityState = securityState,
                            feedbackMessage = pinFeedback,
                            onVerifyPin = viewModel::verifyPin,
                            onRequestBiometric = {
                                launchBiometricPrompt(
                                    onSuccess = viewModel::onBiometricSuccess
                                )
                            },
                            onRecoverPin = viewModel::recoverPinWithPhrase,
                            onHaptic = onHaptic
                        )
                    } else {
                        HardikBudsAppShell(
                            viewModel = viewModel,
                            uiMessage = uiMessage,
                            onHaptic = onHaptic
                        )
                    }

                    AnimatedSplashOverlay(visible = showSplash)
                }
            }
        }
    }

    private fun launchBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        }
        val prompt = BiometricPrompt(this, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock HARDIK BUDS PRO ULTRA")
            .setSubtitle("Verify your identity to access earbuds companion vault")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }
}

@Composable
private fun HardikBudsAppShell(
    viewModel: BudsCompanionViewModel,
    uiMessage: String?,
    onHaptic: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val tokens = LocalLuxuryTokens.current

    val isHardwareSupported by viewModel.isBluetoothHardwareSupported.collectAsStateWithLifecycle()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()
    val bondedDevices by viewModel.bondedDevices.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectedEarbud by viewModel.connectedEarbud.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val securityState by viewModel.securityState.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val batteryHistory by viewModel.batteryHistory.collectAsStateWithLifecycle()
    val connectionEvents by viewModel.connectionEvents.collectAsStateWithLifecycle()
    val registeredDevices by viewModel.registeredDevices.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    val securityLogs by viewModel.securityLogs.collectAsStateWithLifecycle()
    val lastPhoneLocation by viewModel.lastPhoneLocation.collectAsStateWithLifecycle()
    val dynamicIsland by viewModel.dynamicIsland.collectAsStateWithLifecycle()
    val connectionPopup by viewModel.connectionPopup.collectAsStateWithLifecycle()

    var permissionVersion by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionVersion++
        viewModel.refreshBluetooth()
        viewModel.refreshPhoneLocation()
    }

    val hasBtPermissions = remember(permissionVersion) { checkBluetoothPermissions(context) }
    val hasLocationPermission = remember(permissionVersion) { checkLocationPermission(context) }
    val hasNotificationPermission = remember(permissionVersion) { checkNotificationPermission(context) }
    val hasMicPermission = remember(permissionVersion) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(uiMessage) {
        if (!uiMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(uiMessage)
            viewModel.clearUiMessage()
        }
    }

    // Auto-dismiss Dynamic Island pill after 5 seconds
    LaunchedEffect(dynamicIsland) {
        if (dynamicIsland != null) {
            delay(5000L)
            viewModel.dismissDynamicIsland()
        }
    }

    val tabs = remember {
        listOf(
            NavTab(AppRoute.Dashboard, R.string.nav_dashboard, Icons.Rounded.Headphones, Icons.Outlined.Headphones, "nav_dashboard"),
            NavTab(AppRoute.AudioStudio, R.string.nav_audio, Icons.Rounded.Equalizer, Icons.Outlined.Equalizer, "nav_audio"),
            NavTab(AppRoute.SmartControls, R.string.nav_controls, Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_controls"),
            NavTab(AppRoute.FindMy, R.string.nav_find_my, Icons.Rounded.LocationOn, Icons.Outlined.LocationOn, "nav_find_my"),
            NavTab(AppRoute.Security, R.string.nav_security, Icons.Rounded.Security, Icons.Outlined.Security, "nav_security"),
            NavTab(AppRoute.Settings, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val requestCorePermissions = {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(perms)
    }

    AtmosphericBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 680.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = tokens.glassSurface.copy(alpha = 0.94f),
                            tonalElevation = 0.dp
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        onHaptic(false)
                                        navController.navigate(tab.route) {
                                            popUpTo(AppRoute.Dashboard) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = tokens.glassSurface.copy(alpha = 0.85f)
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        onHaptic(false)
                                        navController.navigate(tab.route) {
                                            popUpTo(AppRoute.Dashboard) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.Dashboard
                        ) {
                            composable<AppRoute.Dashboard> {
                                DashboardScreen(
                                    isHardwareSupported = isHardwareSupported,
                                    isBluetoothEnabled = isBluetoothEnabled,
                                    hasBluetoothPermissions = hasBtPermissions,
                                    isScanning = isScanning,
                                    scanStatusMessage = scanStatusMessage,
                                    connectedEarbud = connectedEarbud,
                                    bondedDevices = bondedDevices,
                                    discoveredDevices = discoveredDevices,
                                    batteryHistory = batteryHistory,
                                    maskMac = prefs.maskMacAddressesInUi,
                                    lowBatteryThreshold = prefs.notifications.lowBatteryThreshold,
                                    formatMac = { addr -> viewModel.formatMacForDisplay(addr, prefs.maskMacAddressesInUi) },
                                    onRequestPermissions = requestCorePermissions,
                                    onStartScan = viewModel::startScan,
                                    onStopScan = viewModel::stopScan,
                                    onRefreshBluetooth = viewModel::refreshBluetooth,
                                    onPairDevice = viewModel::pairDevice,
                                    onConnectDevice = viewModel::connectToDevice,
                                    onDisconnectCurrent = viewModel::disconnectCurrent,
                                    onOpenBluetoothSettings = viewModel::openBluetoothSettings,
                                    onPreviewPopup = viewModel::openAnimationPreviewPopup,
                                    onHaptic = onHaptic
                                )
                            }
                            composable<AppRoute.AudioStudio> {
                                AudioStudioScreen(
                                    audioState = audioState,
                                    customPresets = customEqPresets,
                                    hasMicPermission = hasMicPermission,
                                    onRequestMicPermission = {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    },
                                    onApplyPreset = viewModel::applyPreset,
                                    onSetBandDb = viewModel::setEqBand,
                                    onSetBassBoost = viewModel::setBassBoost,
                                    onSetSpatialAudio = viewModel::setSpatialAudio,
                                    onSetSoundMode = viewModel::setSoundMode,
                                    onSaveCustomPreset = viewModel::saveCustomPreset,
                                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                                    onStartChannelTest = viewModel::startChannelTest,
                                    onStopChannelTest = viewModel::stopChannelTest,
                                    onStartMicTest = viewModel::startMicTest,
                                    onStopMicTest = viewModel::stopMicTest,
                                    onHaptic = onHaptic
                                )
                            }
                            composable<AppRoute.SmartControls> {
                                SmartControlsScreen(
                                    connectedEarbud = connectedEarbud,
                                    onOpenCompatibilityMatrix = { navController.navigate(AppRoute.DeviceCompatibility) },
                                    onHaptic = onHaptic
                                )
                            }
                            composable<AppRoute.FindMy> {
                                FindMyScreen(
                                    connectedEarbud = connectedEarbud,
                                    lastPhoneLocation = lastPhoneLocation,
                                    connectionEvents = connectionEvents,
                                    hasLocationPermission = hasLocationPermission,
                                    activeFinderTarget = audioState.activeChannelTest,
                                    isScanningBle = isScanning,
                                    onRequestLocationPermission = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    },
                                    onRefreshLocation = viewModel::refreshPhoneLocation,
                                    onStartFinderSound = viewModel::startFinderSound,
                                    onStopFinderSound = viewModel::stopFinderSound,
                                    onScanRssi = viewModel::startScan,
                                    onHaptic = onHaptic
                                )
                            }
                            composable<AppRoute.Security> {
                                SecurityScreen(
                                    securityState = securityState,
                                    connectedEarbud = connectedEarbud,
                                    registeredDevices = registeredDevices,
                                    connectionEvents = connectionEvents,
                                    securityLogs = securityLogs,
                                    formatMac = { addr -> viewModel.formatMacForDisplay(addr, prefs.maskMacAddressesInUi) },
                                    onConfigurePin = viewModel::configurePin,
                                    onDisablePin = viewModel::disablePin,
                                    onLockAppNow = viewModel::lockAppNow,
                                    onSetBiometricEnabled = viewModel::setBiometricEnabled,
                                    onSetAlertUnauthorized = viewModel::setAlertOnUnauthorized,
                                    onRegisterOwnerDevice = viewModel::registerCurrentAsOwner,
                                    onUnregisterOwnerDevice = viewModel::unregisterOwnerDevice,
                                    onHaptic = onHaptic
                                )
                            }
                            composable<AppRoute.Settings> {
                                SettingsScreen(
                                    prefs = prefs,
                                    hasBluetoothPermissions = hasBtPermissions,
                                    hasLocationPermission = hasLocationPermission,
                                    hasNotificationPermission = hasNotificationPermission,
                                    hasMicPermission = hasMicPermission,
                                    onSetThemeStyle = viewModel::setThemeStyle,
                                    onSetThemeMode = viewModel::setThemeMode,
                                    onSetAccentPalette = viewModel::setAccentPalette,
                                    onSetLanguage = viewModel::setLanguage,
                                    onSetHaptics = viewModel::setHapticsEnabled,
                                    onSetConnectionPopup = viewModel::setConnectionPopupEnabled,
                                    onSetDynamicIsland = viewModel::setDynamicIslandEnabled,
                                    onSetMaskMac = viewModel::setMaskMacAddresses,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onUpdateNotifications = viewModel::updateNotifications,
                                    onRequestPermissions = requestCorePermissions,
                                    onOpenCompatibilityMatrix = { navController.navigate(AppRoute.DeviceCompatibility) },
                                    onClearLogs = viewModel::clearLogs,
                                    onResetSettings = viewModel::resetAllSettings,
                                    onHaptic = onHaptic
                                )
                            }
                            composable<AppRoute.DeviceCompatibility> {
                                DeviceCompatibilityScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Dynamic Island pill
            DynamicIslandPill(
                status = dynamicIsland,
                onDismiss = viewModel::dismissDynamicIsland,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // iOS-inspired Connection Popup Modal
            ConnectionPopupModal(
                state = connectionPopup,
                onDismiss = viewModel::dismissConnectionPopup
            )
        }
    }
}

private fun checkBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
}

private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
