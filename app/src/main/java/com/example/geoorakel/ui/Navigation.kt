package com.example.geoorakel.ui

// regelt die Navigation zwischen Karte und Chat in der navbar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import com.example.geoorakel.viewmodel.LocationViewModel

@Serializable object MapRoute
@Serializable object ChatRoute

@Composable
fun AppNavigation(locationViewModel: LocationViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination // Karte als default Ziel

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<MapRoute>() == true,
                    onClick = { navController.navigate(MapRoute) },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Karte") },
                    label = { Text("Karte") }
                )
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<ChatRoute>() == true,
                    onClick = { navController.navigate(ChatRoute) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                    label = { Text("Chat") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MapRoute
        ) {
            composable<MapRoute> {
                MapScreen(modifier = androidx.compose.ui.Modifier.padding(innerPadding), locationViewModel = locationViewModel)
            }
            composable<ChatRoute> {
                ChatScreen(modifier = androidx.compose.ui.Modifier.padding(innerPadding))
            }
        }
    }
}