package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccentPalette
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hardik_buds_prefs")

data class AppPreferencesState(
    val themeStyle: AppThemeStyle = AppThemeStyle.LIQUID_GLASS,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentPalette: AccentPalette = AccentPalette.LIQUID_CYAN,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hapticsEnabled: Boolean = true,
    val connectionPopupEnabled: Boolean = true,
    val dynamicIslandBannerEnabled: Boolean = true,
    val userProfile: UserProfile = UserProfile(),
    val notifications: NotificationPreferences = NotificationPreferences(),
    val maskMacAddressesInUi: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val CONNECTION_POPUP = booleanPreferencesKey("connection_popup")
        val DYNAMIC_ISLAND = booleanPreferencesKey("dynamic_island")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_BRAND = stringPreferencesKey("profile_brand")
        val MASK_MAC = booleanPreferencesKey("mask_mac")

        val NOTIF_MASTER = booleanPreferencesKey("notif_master")
        val NOTIF_CONNECTED = booleanPreferencesKey("notif_connected")
        val NOTIF_DISCONNECTED = booleanPreferencesKey("notif_disconnected")
        val NOTIF_LOW_BATTERY = booleanPreferencesKey("notif_low_battery")
        val NOTIF_LOW_THRESHOLD = intPreferencesKey("notif_low_threshold")
        val NOTIF_CHARGING = booleanPreferencesKey("notif_charging")
        val NOTIF_BT_OFF = booleanPreferencesKey("notif_bt_off")
        val NOTIF_CONN_LOST = booleanPreferencesKey("notif_conn_lost")
        val NOTIF_UNAUTH = booleanPreferencesKey("notif_unauth")
        val NOTIF_SOUND_URI = stringPreferencesKey("notif_sound_uri")
        val NOTIF_SOUND_NAME = stringPreferencesKey("notif_sound_name")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        AppPreferencesState(
            themeStyle = prefs[Keys.THEME_STYLE]?.let { v ->
                AppThemeStyle.entries.firstOrNull { it.name == v }
            } ?: AppThemeStyle.LIQUID_GLASS,
            themeMode = prefs[Keys.THEME_MODE]?.let { v ->
                ThemeMode.entries.firstOrNull { it.name == v }
            } ?: ThemeMode.DARK,
            accentPalette = prefs[Keys.ACCENT_PALETTE]?.let { v ->
                AccentPalette.entries.firstOrNull { it.name == v }
            } ?: AccentPalette.LIQUID_CYAN,
            language = prefs[Keys.APP_LANGUAGE]?.let { v ->
                AppLanguage.entries.firstOrNull { it.code == v }
            } ?: AppLanguage.ENGLISH,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            connectionPopupEnabled = prefs[Keys.CONNECTION_POPUP] ?: true,
            dynamicIslandBannerEnabled = prefs[Keys.DYNAMIC_ISLAND] ?: true,
            userProfile = UserProfile(
                displayName = prefs[Keys.PROFILE_NAME] ?: "Hardik Listener",
                preferredBrandLabel = prefs[Keys.PROFILE_BRAND] ?: "Universal"
            ),
            maskMacAddressesInUi = prefs[Keys.MASK_MAC] ?: false,
            notifications = NotificationPreferences(
                masterEnabled = prefs[Keys.NOTIF_MASTER] ?: true,
                onConnected = prefs[Keys.NOTIF_CONNECTED] ?: true,
                onDisconnected = prefs[Keys.NOTIF_DISCONNECTED] ?: true,
                onLowBattery = prefs[Keys.NOTIF_LOW_BATTERY] ?: true,
                lowBatteryThreshold = prefs[Keys.NOTIF_LOW_THRESHOLD] ?: 20,
                onChargingChanged = prefs[Keys.NOTIF_CHARGING] ?: true,
                onBluetoothDisabled = prefs[Keys.NOTIF_BT_OFF] ?: true,
                onConnectionLost = prefs[Keys.NOTIF_CONN_LOST] ?: true,
                onUnauthorizedAccess = prefs[Keys.NOTIF_UNAUTH] ?: true,
                customSoundUri = prefs[Keys.NOTIF_SOUND_URI],
                customSoundName = prefs[Keys.NOTIF_SOUND_NAME] ?: "Default System Sound"
            )
        )
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        context.dataStore.edit { it[Keys.THEME_STYLE] = style.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPalette(accent: AccentPalette) {
        context.dataStore.edit { it[Keys.ACCENT_PALETTE] = accent.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.getSharedPreferences("hardik_buds_locale", Context.MODE_PRIVATE)
            .edit()
            .putString("lang_code", language.code)
            .apply()
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.code }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setConnectionPopupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONNECTION_POPUP] = enabled }
    }

    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_ISLAND] = enabled }
    }

    suspend fun setMaskMacAddresses(masked: Boolean) {
        context.dataStore.edit { it[Keys.MASK_MAC] = masked }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        context.dataStore.edit {
            it[Keys.PROFILE_NAME] = profile.displayName
            it[Keys.PROFILE_BRAND] = profile.preferredBrandLabel
        }
    }

    suspend fun updateNotifications(prefs: NotificationPreferences) {
        context.dataStore.edit {
            it[Keys.NOTIF_MASTER] = prefs.masterEnabled
            it[Keys.NOTIF_CONNECTED] = prefs.onConnected
            it[Keys.NOTIF_DISCONNECTED] = prefs.onDisconnected
            it[Keys.NOTIF_LOW_BATTERY] = prefs.onLowBattery
            it[Keys.NOTIF_LOW_THRESHOLD] = prefs.lowBatteryThreshold
            it[Keys.NOTIF_CHARGING] = prefs.onChargingChanged
            it[Keys.NOTIF_BT_OFF] = prefs.onBluetoothDisabled
            it[Keys.NOTIF_CONN_LOST] = prefs.onConnectionLost
            it[Keys.NOTIF_UNAUTH] = prefs.onUnauthorizedAccess
            if (prefs.customSoundUri != null) {
                it[Keys.NOTIF_SOUND_URI] = prefs.customSoundUri
            } else {
                it.remove(Keys.NOTIF_SOUND_URI)
            }
            it[Keys.NOTIF_SOUND_NAME] = prefs.customSoundName
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
