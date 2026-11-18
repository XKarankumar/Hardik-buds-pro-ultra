package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import com.example.domain.model.SecurityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreSecurityManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "HardikBudsProMasterKey_v1"
        private const val PREFS_NAME = "hardik_buds_secure_vault"

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT_ENC = "pin_salt_enc"
        private const val KEY_PIN_SALT_IV = "pin_salt_iv"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_UNAUTH_COUNT = "unauth_count"
        private const val KEY_ALERT_UNAUTH = "alert_unauth"
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    private fun loadInitialState(): SecurityState {
        val pinConfigured = prefs.contains(KEY_PIN_HASH)
        return SecurityState(
            isPinConfigured = pinConfigured,
            pinLength = prefs.getInt(KEY_PIN_LENGTH, 4),
            isBiometricHardwareAvailable = checkBiometricAvailable(),
            isBiometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
            isAppLocked = pinConfigured,
            failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilMillis = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L),
            isRecoveryConfigured = prefs.contains(KEY_RECOVERY_HASH),
            alertOnUnauthorizedAccess = prefs.getBoolean(KEY_ALERT_UNAUTH, true),
            unauthorizedAttemptCount = prefs.getInt(KEY_UNAUTH_COUNT, 0)
        )
    }

    fun checkBiometricAvailable(): Boolean {
        return try {
            val manager = BiometricManager.from(context)
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    fun configurePin(pin: String, recoveryPhrase: String? = null): Boolean {
        if (pin.length !in listOf(4, 6) || !pin.all { it.isDigit() }) return false
        val saltBytes = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val saltB64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = sha256Hex(saltB64 + ":" + pin)

        val (encSalt, iv) = encryptWithKeystore(saltB64)
        val editor = prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT_ENC, encSalt)
            .putString(KEY_PIN_SALT_IV, iv)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)

        if (!recoveryPhrase.isNullOrBlank()) {
            val normalized = recoveryPhrase.trim().lowercase()
            editor.putString(KEY_RECOVERY_HASH, sha256Hex(saltB64 + ":rec:" + normalized))
        }
        editor.apply()

        _state.value = _state.value.copy(
            isPinConfigured = true,
            pinLength = pin.length,
            isAppLocked = false,
            failedAttempts = 0,
            lockoutUntilMillis = 0L,
            isRecoveryConfigured = prefs.contains(KEY_RECOVERY_HASH)
        )
        return true
    }

    fun disablePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT_ENC)
            .remove(KEY_PIN_SALT_IV)
            .remove(KEY_RECOVERY_HASH)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()

        _state.value = _state.value.copy(
            isPinConfigured = false,
            isBiometricEnabled = false,
            isAppLocked = false,
            failedAttempts = 0,
            lockoutUntilMillis = 0L,
            isRecoveryConfigured = false
        )
    }

    sealed interface PinVerifyResult {
        data object Success : PinVerifyResult
        data class Invalid(val remainingBeforeLockout: Int) : PinVerifyResult
        data class LockedOut(val remainingSeconds: Long) : PinVerifyResult
    }

    fun verifyPin(pin: String, nowMillis: Long = System.currentTimeMillis()): PinVerifyResult {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (nowMillis < lockoutUntil) {
            val sec = ((lockoutUntil - nowMillis) / 1000L).coerceAtLeast(1L)
            return PinVerifyResult.LockedOut(sec)
        }

        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return PinVerifyResult.Success
        val encSalt = prefs.getString(KEY_PIN_SALT_ENC, null)
        val iv = prefs.getString(KEY_PIN_SALT_IV, null)
        val salt = if (encSalt != null && iv != null) decryptWithKeystore(encSalt, iv) else null
        val candidateHash = if (salt != null) sha256Hex("$salt:$pin") else null

        return if (candidateHash != null && MessageDigest.isEqual(candidateHash.toByteArray(), savedHash.toByteArray())) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
            _state.value = _state.value.copy(isAppLocked = false, failedAttempts = 0, lockoutUntilMillis = 0L)
            PinVerifyResult.Success
        } else {
            val fails = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val unauth = prefs.getInt(KEY_UNAUTH_COUNT, 0) + 1
            var newLockout = 0L
            if (fails >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                val multiplier = (fails - MAX_ATTEMPTS_BEFORE_LOCKOUT + 1).coerceAtMost(5)
                val lockoutSeconds = 30L * multiplier
                newLockout = nowMillis + (lockoutSeconds * 1000L)
            }
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, fails)
                .putInt(KEY_UNAUTH_COUNT, unauth)
                .putLong(KEY_LOCKOUT_UNTIL, newLockout)
                .apply()
            _state.value = _state.value.copy(
                failedAttempts = fails,
                lockoutUntilMillis = newLockout,
                unauthorizedAttemptCount = unauth
            )
            if (newLockout > nowMillis) {
                PinVerifyResult.LockedOut((newLockout - nowMillis) / 1000L)
            } else {
                PinVerifyResult.Invalid((MAX_ATTEMPTS_BEFORE_LOCKOUT - fails).coerceAtLeast(0))
            }
        }
    }

    fun recoverWithPhrase(phrase: String, newPin: String): Boolean {
        if (newPin.length !in listOf(4, 6) || !newPin.all { it.isDigit() }) return false
        val savedRecovery = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        val encSalt = prefs.getString(KEY_PIN_SALT_ENC, null) ?: return false
        val iv = prefs.getString(KEY_PIN_SALT_IV, null) ?: return false
        val salt = decryptWithKeystore(encSalt, iv) ?: return false
        val candidate = sha256Hex("$salt:rec:${phrase.trim().lowercase()}")
        return if (MessageDigest.isEqual(candidate.toByteArray(), savedRecovery.toByteArray())) {
            configurePin(newPin, phrase)
            true
        } else {
            false
        }
    }

    fun unlockViaBiometric() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
        _state.value = _state.value.copy(isAppLocked = false, failedAttempts = 0, lockoutUntilMillis = 0L)
    }

    fun lockAppNow() {
        if (_state.value.isPinConfigured) {
            _state.value = _state.value.copy(isAppLocked = true)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _state.value = _state.value.copy(isBiometricEnabled = enabled)
    }

    fun setAlertOnUnauthorizedAccess(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALERT_UNAUTH, enabled).apply()
        _state.value = _state.value.copy(alertOnUnauthorizedAccess = enabled)
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            null
        }
    }

    private fun encryptWithKeystore(plainText: String): Pair<String, String> {
        val secretKey = getOrCreateKeystoreSecretKey()
        if (secretKey == null) {
            // Fallback for Robolectric JVM test environment where AndroidKeyStore is absent
            return Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) to "NO_KEYSTORE"
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decryptWithKeystore(cipherB64: String, ivB64: String): String? {
        return try {
            if (ivB64 == "NO_KEYSTORE") {
                return String(Base64.decode(cipherB64, Base64.NO_WRAP), Charsets.UTF_8)
            }
            val secretKey = getOrCreateKeystoreSecretKey() ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plain = cipher.doFinal(Base64.decode(cipherB64, Base64.NO_WRAP))
            String(plain, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
