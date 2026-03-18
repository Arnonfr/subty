package com.subtranslate.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.subtranslate.presentation.history.HistoryScreen
import com.subtranslate.presentation.results.ResultsScreen
import com.subtranslate.presentation.search.SearchScreen
import com.subtranslate.presentation.settings.SettingsScreen
import com.subtranslate.presentation.translate.TranslateScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Search.route,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {

        composable(Screen.Search.route) {
            SearchScreen(
                onSearch = { query -> navController.navigate(Screen.Results.createRoute(query)) }
            )
        }

        composable(
            route = Screen.Results.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStack ->
            val query = backStack.arguments?.getString("query") ?: ""
            ResultsScreen(
                query = query,
                onTranslate = { fileId, fileName, languageCode ->
                    navController.navigate(Screen.Translate.createRoute(fileId, fileName, languageCode))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Translate.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.IntType },
                navArgument("fileName") { type = NavType.StringType },
                navArgument("languageCode") { type = NavType.StringType }
            )
        ) { backStack ->
            val fileId = backStack.arguments?.getInt("fileId") ?: 0
            val fileName = backStack.arguments?.getString("fileName") ?: ""
            val languageCode = backStack.arguments?.getString("languageCode") ?: "en"
            TranslateScreen(
                fileId = fileId,
                fileName = fileName,
                languageCode = languageCode,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onSearchAgain = { item ->
                    navController.navigate(Screen.Results.createRoute(item.query))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
