package io.github.siddhantpardeshi.aetherplay.space

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.siddhantpardeshi.aetherplay.design.AetherTheme
import io.github.siddhantpardeshi.aetherplay.design.AppIdentity
import io.github.siddhantpardeshi.aetherplay.design.IcBridge
import io.github.siddhantpardeshi.aetherplay.design.IcCrystal
import io.github.siddhantpardeshi.aetherplay.design.IcLayers
import io.github.siddhantpardeshi.aetherplay.design.IcOrbit
import io.github.siddhantpardeshi.aetherplay.design.LocalAccent
import io.github.siddhantpardeshi.aetherplay.design.LocalPalette
import io.github.siddhantpardeshi.aetherplay.design.MonoValueStyle
import io.github.siddhantpardeshi.aetherplay.design.ThemeMode
import io.github.siddhantpardeshi.aetherplay.design.auroraBackdrop
import io.github.siddhantpardeshi.aetherplay.protocol.SessionState
import io.github.siddhantpardeshi.aetherplay.space.ui.LibraryScreen
import io.github.siddhantpardeshi.aetherplay.space.ui.SessionScreen
import io.github.siddhantpardeshi.aetherplay.space.ui.SettingsScreen
import io.github.siddhantpardeshi.aetherplay.space.ui.SpacesScreen

class MainActivity : ComponentActivity() {

    private val vm: SpaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeMode = Prefs.themeMode(this)
        var reduceMotion = Prefs.reduceMotion(this)
        setContent {
            var mode by remember { mutableStateOf(themeMode) }
            var motion by remember { mutableStateOf(reduceMotion) }
            AetherTheme(mode = mode, identity = AppIdentity.SPACE, reduceMotion = motion) {
                SpaceRoot(
                    vm = vm,
                    onThemeChange = { mode = it; themeMode = it },
                    onMotionChange = { motion = it; reduceMotion = it },
                )
            }
        }
    }
}

private data class SpaceTab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun SpaceRoot(vm: SpaceViewModel, onThemeChange: (ThemeMode) -> Unit, onMotionChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    val nav = rememberNavController()
    val session = vm.session.collectAsState().value
    val tabs = listOf(
        SpaceTab("library", "Library", IcCrystal),
        SpaceTab("spaces", "Spaces", IcLayers),
        SpaceTab("session", "Session", IcOrbit),
        SpaceTab("settings", "Settings", IcBridge),
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Box(Modifier.fillMaxSize().auroraBackdrop(intensity = 0.8f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = nav,
                    startDestination = "library",
                    enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
                    exitTransition = { fadeOut(tween(160)) },
                ) {
                    composable("library") {
                        ScreenScaffold("AetherPlay Space", "The stage for your games") {
                            LibraryScreen(
                                vm = vm,
                                onOpenSession = { nav.navigate("session") },
                                onOpenSpaces = { nav.navigate("spaces") },
                            )
                        }
                    }
                    composable("spaces") {
                        ScreenScaffold("Spaces", "Separate save data per space") {
                            SpacesScreen(vm, activeSpaceKey = session.spaceKey)
                        }
                    }
                    composable("session") {
                        ScreenScaffold("Session", "Host a game for AetherPlay Lab") {
                            SessionScreen(vm)
                        }
                    }
                    composable("settings") {
                        ScreenScaffold("Settings", "Appearance and about") {
                            SettingsScreen(vm, onThemeChange, onMotionChange)
                        }
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
        if (session.state == SessionState.RUNNING) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(palette.bg.copy(alpha = 0.9f), palette.bg.copy(alpha = 0f))))
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun ScreenScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalAccent.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("AETHERPLAY", style = MonoValueStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize), color = accent.accent)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, style = MaterialTheme.typography.displaySmall, color = palette.textPrimary)
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}
