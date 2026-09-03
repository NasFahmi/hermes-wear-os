package com.hermes.wearos.presentation.navigation

sealed class WearScreen(val route: String) {
    data object Home : WearScreen("home")
    data object Settings : WearScreen("settings")
}
