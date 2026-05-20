package com.example.geoorakel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.geoorakel.theme.GeoOrkaelTheme
import com.example.geoorakel.ui.AppNavigation
import com.example.geoorakel.ui.CheckLocationPermission
import com.example.geoorakel.viewmodel.LocationViewModel

class MainActivity : ComponentActivity() {
    private val locationViewModel: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        org.maplibre.android.MapLibre.getInstance(applicationContext) // Karte initialisieren
        enableEdgeToEdge()
        setContent {
            GeoOrkaelTheme {
                // überprüfe, ob Standort-Berechtigung erteilt wurde
                var hasPermission by remember { mutableStateOf(false) }

                if (hasPermission) {
                    // UI wird geladen
                    AppNavigation(locationViewModel = locationViewModel)
                } else {
                    // Berechtigung wird geprüft
                    CheckLocationPermission(
                        onGranted = {
                            locationViewModel.startLocationUpdates()
                            hasPermission = true
                        }
                    )
                }
            }
        }
    }
}