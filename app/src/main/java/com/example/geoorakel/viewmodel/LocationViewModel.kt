package com.example.geoorakel.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geoorakel.data.LocationRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _oraclePrompt = MutableStateFlow<String?>(null)
    val oraclePrompt: StateFlow<String?> = _oraclePrompt.asStateFlow()

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    _location.value = it
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            android.os.Looper.getMainLooper()
        )
    }

    fun buildOraclePrompt(lat: Double, lon: Double) {
        viewModelScope.launch {
            val placeName = locationRepository.getAddressName(lat, lon)
            val wikiContext = locationRepository.getNearbyWikiContext(lat, lon)

            val contextBlock = if (wikiContext.isNotBlank()) {
                "\n\nHier sind bekannte Orte/Themen aus der näheren Umgebung laut Wikipedia:\n\n$wikiContext"
            } else {
                ""
            }

            _oraclePrompt.value =
                "Ich befinde mich hier: $placeName (Koordinaten: $lat, $lon)." +
                        contextBlock +
                        "\n\nDu bist das GeoOrakel. Erzähle mir, welche interessanten Orte es hier gibt."
        }
    }

    fun clearOraclePrompt() {
        _oraclePrompt.value = null
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}