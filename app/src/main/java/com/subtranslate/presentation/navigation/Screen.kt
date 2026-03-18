package com.subtranslate.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Search : Screen("search")
    object Results : Screen("results/{query}") {
        fun createRoute(query: String) = "results/${Uri.encode(query)}"
    }
    // languageCode added so TranslateViewModel can auto-detect source language
    object Translate : Screen("translate/{fileId}/{fileName}/{languageCode}") {
        fun createRoute(fileId: Int, fileName: String, languageCode: String) =
            "translate/$fileId/${Uri.encode(fileName)}/$languageCode"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
}
