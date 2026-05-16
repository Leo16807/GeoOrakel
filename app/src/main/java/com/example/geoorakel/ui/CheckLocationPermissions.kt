package com.example.geoorakel.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckLocationPermission(
    onGranted: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var permissionRequested by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    when {
        // Fall 1: Berechtigung erteilt
        permissionState.allPermissionsGranted -> {
            onGranted()
        }

        // Fall 2: Dauerhaft verweigert
        permissionRequested && !permissionState.shouldShowRationale && !permissionState.allPermissionsGranted -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Standortberechtigung dauerhaft verweigert") },
                text = { Text("Bitte erteile die Berechtigung manuell in den Einstellungen.") },
                confirmButton = {
                    Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity?.packageName, null)
                        )
                        activity?.startActivity(intent)
                    }) {
                        Text("Einstellungen öffnen")
                    }
                },
                dismissButton = {
                    Button(onClick = { activity?.finish() }) {
                        Text("App beenden")
                    }
                }
            )
        }

        // Fall 3: Erste Anfrage oder einmal abgelehnt
        else -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Standortberechtigung") },
                text = {
                    if (permissionState.shouldShowRationale) {
                        Text("Du hast die Berechtigung zuvor abgelehnt. Ohne Standort kann die Karte nicht genutzt werden.")
                    } else {
                        Text("Diese App benötigt Zugriff auf deinen Standort um die Karte anzuzeigen.")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        permissionRequested = true
                        permissionState.launchMultiplePermissionRequest()
                    }) {
                        Text("Berechtigung anfragen")
                    }
                },
                dismissButton = {
                    Button(onClick = { activity?.finish() }) {
                        Text("App beenden")
                    }
                }
            )
        }
    }
}