package com.neomaverick.stride.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neomaverick.stride.ui.screens.HabitsScreen
import com.neomaverick.stride.ui.screens.InsightsScreen
import com.neomaverick.stride.ui.screens.SettingsScreen
import com.neomaverick.stride.ui.screens.TodayScreen
import com.neomaverick.stride.ui.screens.WorkoutsScreen
import com.neomaverick.stride.ui.theme.StrideColors

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Rounded.Today),
    HABITS("habits", "Habits", Icons.Rounded.CheckCircle),
    MOVE("move", "Workouts", Icons.Rounded.FitnessCenter),
    COACH("coach", "Coach", Icons.Rounded.AutoAwesome),
    YOU("you", "You", Icons.Rounded.Person),
}

@Composable
fun StrideRoot(vm: StrideViewModel, start: Tab = Tab.TODAY) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = StrideColors.Surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = entry?.destination?.route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, null) }, label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = StrideColors.Lime.copy(alpha = 0.18f),
                            selectedIconColor = StrideColors.Lime, selectedTextColor = StrideColors.Lime),
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, start.route, Modifier.padding(pad)) {
            composable(Tab.TODAY.route) { TodayScreen(vm) }
            composable(Tab.HABITS.route) { HabitsScreen(vm) }
            composable(Tab.MOVE.route) { WorkoutsScreen(vm) }
            composable(Tab.COACH.route) { InsightsScreen(vm) }
            composable(Tab.YOU.route) { SettingsScreen(vm) }
        }
    }
}
