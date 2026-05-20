package com.example.geoorakel.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {

    suspend fun getAddressName(lat: Double, lon: Double): String =
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())
            val defaultName = "Koordinaten: $lat, $lon"

            fun buildName(address: Address): String {
                val thoroughfare = address.thoroughfare
                val subThoroughfare = address.subThoroughfare
                val subLocality = address.subLocality
                val locality = address.locality

                return when {
                    thoroughfare != null && subThoroughfare != null ->
                        "$thoroughfare $subThoroughfare, ${subLocality ?: ""}, ${locality ?: ""}".trimEnd(',', ' ')
                    thoroughfare != null ->
                        "$thoroughfare, ${subLocality ?: ""}, ${locality ?: ""}".trimEnd(',', ' ')
                    subLocality != null && locality != null ->
                        "$subLocality, $locality"
                    else ->
                        locality ?: subLocality ?: address.featureName ?: defaultName
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    val name = addresses.firstOrNull()?.let { buildName(it) } ?: defaultName
                    continuation.resume(name)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val name = addresses?.firstOrNull()?.let { buildName(it) } ?: defaultName
                continuation.resume(name)
            }
        }
}