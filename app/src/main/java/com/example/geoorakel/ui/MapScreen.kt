package com.example.geoorakel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.geoorakel.BuildConfig
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import androidx.compose.runtime.*
import com.example.geoorakel.viewmodel.LocationViewModel
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import org.maplibre.android.camera.CameraUpdateFactory


@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    locationViewModel: LocationViewModel,
    onAskOracle: (String) -> Unit) {

    val location by locationViewModel.location.collectAsState() // Standort aus LocationViewModel.kt
    val currentLocation by rememberUpdatedState(location)
    val oraclePrompt by locationViewModel.oraclePrompt.collectAsState()

    val context = LocalContext.current
    val styleUrl = "https://api.maptiler.com/maps/outdoor/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

    var mapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    val hasCentered = remember { mutableStateOf(false) }

    // Kartenobjekt
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                mapRef = map
                map.setStyle(styleUrl) { style ->
                    val locationComponent = map.locationComponent
                    locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions
                            .builder(context, style)
                            .build()
                    )
                    locationComponent.isLocationComponentEnabled = true
                    locationComponent.cameraMode = CameraMode.NONE
                    locationComponent.renderMode = RenderMode.NORMAL
                }
                map.setMinZoomPreference(4.0)
                map.setMaxZoomPreference(18.0)
            }
        }
    }

    // Beim ersten Location-Update auf den Standort zoomen
    LaunchedEffect(location, mapRef) {
        android.util.Log.d("MapScreen", "LaunchedEffect: location=$location, mapRef=$mapRef, hasCentered=${hasCentered.value}")
        if (!hasCentered.value && location != null && mapRef != null) {
            zoomToCurrentLocation(mapRef!!, location!!.latitude, location!!.longitude)
            hasCentered.value = true
        }
    }

    // Karten Lifecycle behandeln
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(oraclePrompt) {
        oraclePrompt?.let {
            onAskOracle(it)
            locationViewModel.clearOraclePrompt()
        }
    }

    // Layout
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )
        // Orakel fragen (später)
        Button(
            onClick = {
                val loc = currentLocation
                if (loc != null) {
                    locationViewModel.buildOraclePrompt(loc.latitude, loc.longitude)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Text("Orakel fragen")
        }

        // Button um auf eigenen Standort zu zoomen
        Button(
            onClick = {
                val loc = currentLocation
                val map = mapRef
                if (loc != null && map != null) {
                    zoomToCurrentLocation(map, loc.latitude, loc.longitude)
                }
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(56.dp)
        ) {
            Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "Auf Standort zoomen"
        )
        }
    }
}

// auf Standort zoomen
fun zoomToCurrentLocation(map: org.maplibre.android.maps.MapLibreMap, latitude: Double, longitude: Double) {
    android.util.Log.d("MapScreen", "Zoome auf: $latitude, $longitude")
    val cameraPosition = CameraPosition.Builder()
        .target(LatLng(latitude, longitude))
        .zoom(15.0)
        .bearing(0.0)
        .build()

    map.animateCamera(
        CameraUpdateFactory.newCameraPosition(cameraPosition),
        1000
    )
}
