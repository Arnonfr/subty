package com.subtranslate.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.subtranslate.presentation.history.HistoryScreen
import com.subtranslate.presentation.preview.PreviewScreen
import com.subtranslate.presentation.results.ResultsScreen
import com.subtranslate.presentation.search.SearchScreen
import com.subtranslate.presentation.settings.SettingsScreen
import com.subtranslate.presentation.translate.TranslateScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Search.route) {

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
                onSubtitleSelected = { fileId, fileName ->
                    navController.navigate(Screen.Preview.createRoute(fileId, fileName))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                navArgument("fileId") { type = NavType.IntType },
                navArgument("fileName") { type = NavType.StringType }
            )
        ) { backStack ->
            val fileId = backStack.arguments?.getInt("fileId") ?: 0
            val fileName = backStack.arguments?.getString("fileName") ?: ""
            PreviewScreen(
                fileId = fileId,
                fileName = fileName,
                onTranslate = { navController.navigate(Screen.Translate.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Translate.route) {
            TranslateScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
