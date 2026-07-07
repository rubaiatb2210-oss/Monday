package dev.monday.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.monday.ui.assistant.AssistantScreen
import dev.monday.ui.dashboard.DashboardScreen
import dev.monday.ui.devmode.DevModeScreen
import dev.monday.ui.navigation.Screen
import dev.monday.ui.reminders.RemindersScreen
import dev.monday.ui.settings.SettingsScreen
import dev.monday.ui.theme.*
import dev.monday.ui.timeline.TimelineScreen
import dev.monday.ui.voice.VoiceScreen

@Composable
fun MondayApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on settings and devmode screens
    val showBottomBar = currentDestination?.route in
            Screen.bottomBarItems.map { it.route }

    Scaffold(
        containerColor = MondayNavy,
        bottomBar = {
            if (showBottomBar) {
                MondayBottomBar(
                    currentRoute = currentDestination?.route,
                    onNavigate = { screen ->
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
            )
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Voice.route) {
                VoiceScreen()
            }
            composable(Screen.Assistant.route) {
                AssistantScreen()
            }
            composable(Screen.Timeline.route) {
                TimelineScreen()
            }
            composable(Screen.Reminders.route) {
                RemindersScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDevMode = { navController.navigate(Screen.DevMode.route) }
                )
            }
            composable(Screen.DevMode.route) {
                DevModeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MondayBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MondayNavyLight,
        contentColor = MondayTextSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Screen.bottomBarItems.forEach { screen ->
            val selected = currentRoute == screen.route

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MondayBlue,
                    selectedTextColor = MondayBlue,
                    unselectedIconColor = MondayTextMuted,
                    unselectedTextColor = MondayTextMuted,
                    indicatorColor = MondayBlue.copy(alpha = 0.12f)
                )
            )
        }
    }
}
