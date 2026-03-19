package com.subtranslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subtranslate.data.local.datastore.SettingsDataStore
import com.subtranslate.data.remote.config.AppConfig
import com.subtranslate.data.remote.config.RemoteConfigManager
import com.subtranslate.presentation.maintenance.MaintenanceScreen
import com.subtranslate.presentation.navigation.NavGraph
import com.subtranslate.presentation.navigation.Screen
import com.subtranslate.presentation.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Search,   "Search",   Icons.Default.Search),
    NavItem(Screen.History,  "History",  Icons.Default.History),
    NavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

/** Three-state config load: loading / loaded+enabled / loaded+disabled */
private sealed interface ConfigState {
    object Loading : ConfigState
    data class Ready(val config: AppConfig) : ConfigState
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var remoteConfigManager: RemoteConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by settingsDataStore.isDarkThemeFlow.collectAsState()

            // Fetch Remote Config once on launch
            var configState by remember { mutableStateOf<ConfigState>(ConfigState.Loading) }
            LaunchedEffect(Unit) {
                val config = remoteConfigManager.fetchAndGet()
                configState = ConfigState.Ready(config)
            }

            SubTranslateTheme(darkTheme = isDark) {
                when (val state = configState) {

                    // ── Splash / loading ──────────────────────────────────────
                    ConfigState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SubtyBg)
                                .windowInsetsPadding(WindowInsets.systemBars),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = SubtyMocha, strokeWidth = 1.5.dp)
                        }
                    }

                    is ConfigState.Ready -> {
                        if (!state.config.appEnabled) {
                            // ── Maintenance screen ────────────────────────────
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.systemBars),
                            ) {
                                MaintenanceScreen(message = state.config.maintenanceMessage)
                            }
                        } else {
                            // ── Normal app ────────────────────────────────────
                            val navController = rememberNavController()
                            val backStack by navController.currentBackStackEntryAsState()
                            val currentRoute = backStack?.destination?.route
                            val showBottomBar = NAV_ITEMS.any { it.screen.route == currentRoute }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SubtyBg)
                                    .windowInsetsPadding(WindowInsets.systemBars),
                            ) {
                                NavGraph(
                                    navController = navController,
                                    modifier = Modifier.weight(1f),
                                )

                                if (showBottomBar) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(SubtyBorder))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .background(SubtyBg),
                                    ) {
                                        NAV_ITEMS.forEachIndexed { i, item ->
                                            val selected = currentRoute == item.screen.route
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(if (selected) SubtyBorder else SubtyBg)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                    ) {
                                                        navController.navigate(item.screen.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.label,
                                                        tint = if (selected) SubtyBg else SubtyText3,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    SubtyLabel(
                                                        item.label,
                                                        color = if (selected) SubtyBg else SubtyText3,
                                                    )
                                                }
                                            }
                                            if (i < NAV_ITEMS.lastIndex) {
                                                Box(Modifier.width(1.dp).fillMaxHeight().background(SubtyBorder))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
