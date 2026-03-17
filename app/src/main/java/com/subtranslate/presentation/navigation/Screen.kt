package com.subtranslate.presentation.navigation

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Results : Screen("results/{query}") {
        fun createRoute(query: String) = "results/$query"
    }
    object Preview : Screen("preview/{fileId}/{fileName}") {
        fun createRoute(fileId: Int, fileName: String) = "preview/$fileId/${fileName}"
    }
    object Translate : Screen("translate")
    object History : Screen("history")
    object Settings : Screen("settings")
}
