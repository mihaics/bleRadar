package com.bleradar.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bleradar.ui.screens.AnalyticsScreen
import com.bleradar.ui.screens.DeviceListScreen
import com.bleradar.ui.screens.DeviceDetailScreen
import com.bleradar.ui.screens.MapScreen
import com.bleradar.ui.screens.SettingsScreen
import com.bleradar.ui.screens.AlertsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleRadarNavigation() {
    val navController = rememberNavController()
    
    val items = listOf(
        Screen.DeviceList,
        Screen.Map,
        Screen.Analytics,
        Screen.Alerts,
        Screen.Settings
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Radar") }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { 
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.DeviceList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.DeviceList.route) {
                DeviceListScreen(
                    onDeviceClick = { deviceAddress ->
                        navController.navigate("device_detail/$deviceAddress")
                    }
                )
            }
            composable("device_detail/{deviceAddress}") { backStackEntry ->
                val deviceAddress = backStackEntry.arguments?.getString("deviceAddress") ?: ""
                DeviceDetailScreen(
                    deviceAddress = deviceAddress,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onShowOnMap = { address ->
                        navController.navigate("${Screen.Map.route}?deviceAddress=$address")
                    }
                )
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable("${Screen.Map.route}?deviceAddress={deviceAddress}") { backStackEntry ->
                val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
                MapScreen(
                    focusedDeviceAddress = deviceAddress
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Alerts.route) {
                AlertsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object DeviceList : Screen("device_list", "Devices", Icons.Default.List)
    object Map : Screen("map", "Map", Icons.Default.LocationOn)
    object Analytics : Screen("analytics", "Stats", Icons.Default.Info)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Warning)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}