package com.subtranslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subtranslate.presentation.navigation.NavGraph
import com.subtranslate.presentation.navigation.Screen
import com.subtranslate.presentation.theme.*
import dagger.hilt.android.AndroidEntryPoint

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Search,   "Search",   Icons.Default.Search),
    NavItem(Screen.History,  "History",  Icons.Default.History),
    NavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubTranslateTheme {
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
                        // Top border of nav bar
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SubtyBorder))
                        // Nav items
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
                                            tint = if (selected) SubtyBlack else SubtyText3,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        SubtyLabel(
                                            item.label,
                                            color = if (selected) SubtyBlack else SubtyText3,
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
