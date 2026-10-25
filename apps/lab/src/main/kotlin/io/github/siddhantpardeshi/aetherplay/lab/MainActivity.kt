package io.github.siddhantpardeshi.aetherplay.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.siddhantpardeshi.aetherplay.design.AetherTheme
import io.github.siddhantpardeshi.aetherplay.design.AppIdentity
import io.github.siddhantpardeshi.aetherplay.design.IcBridge
import io.github.siddhantpardeshi.aetherplay.design.IcLayers
import io.github.siddhantpardeshi.aetherplay.design.IcScan
import io.github.siddhantpardeshi.aetherplay.design.IcSpark
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.ThemeMode
import io.github.siddhantpardeshi.aetherplay.design.auroraBackdrop
import io.github.siddhantpardeshi.aetherplay.lab.ui.HomeScreen
import io.github.siddhantpardeshi.aetherplay.lab.ui.HistoryScreen
import io.github.siddhantpardeshi.aetherplay.lab.ui.MoreScreen
import io.github.siddhantpardeshi.aetherplay.lab.ui.SearchScreen

class MainActivity : ComponentActivity() {

    val vm: LabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var mode by remember { mutableStateOf(Prefs.themeMode(this)) }
            var motion by remember { mutableStateOf(Prefs.reduceMotion(this)) }
            AetherTheme(mode = mode, identity = AppIdentity.LAB, reduceMotion = motion) {
                LabRoot(
                    vm = vm,
                    onThemeChange = { m -> Prefs.setThemeMode(this, m); mode = m },
                    onMotionChange = { v -> Prefs.setReduceMotion(this, v); motion = v },
                )
            }
        }
    }
}

private data class LabTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun LabRoot(vm: LabViewModel, onThemeChange: (ThemeMode) -> Unit, onMotionChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val nav = rememberNavController()
    val tabs = listOf(
        LabTab("home", "Home", IcBridge),
        LabTab("search", "Search", IcScan),
        LabTab("history", "History", IcLayers),
        LabTab("more", "More", IcSpark),
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(Unit) {
        vm.conn.collect { }
    }

    Box(Modifier.fillMaxSize().auroraBackdrop(intensity = 0.8f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
                    exitTransition = { fadeOut(tween(160)) },
                ) {
                    composable("home") { HomeScreen(vm, onOpenSearch = { nav.navigate("search") }) }
                    composable("search") { SearchScreen(vm) }
                    composable("history") { HistoryScreen(vm) }
                    composable("more") {
                        MoreScreen(vm, onThemeChange = onThemeChange, onMotionChange = onMotionChange)
                    }
                }
            }
            NavigationBar(
                containerColor = palette.base,
                contentColor = palette.textPrimary,
                tonalElevation = 0.dp,
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent.accent,
                            selectedTextColor = accent.accent,
                            unselectedIconColor = palette.textSecondary,
                            unselectedTextColor = palette.textSecondary,
                            indicatorColor = accent.accent.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        }
    }
}
