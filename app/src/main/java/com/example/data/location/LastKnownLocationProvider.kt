package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class PhoneLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

class LastKnownLocationProvider(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownPhoneLocation(): PhoneLocationSnapshot? {
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            val loc = try {
                if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
            } catch (_: Exception) {
                null
            }
            if (loc != null && (best == null || loc.time > best.time)) {
                best = loc
            }
        }
        return best?.let {
            PhoneLocationSnapshot(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = it.accuracy,
                timestampMillis = it.time
            )
        }
    }
}
