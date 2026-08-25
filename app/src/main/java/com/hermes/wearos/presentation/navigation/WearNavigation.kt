package com.hermes.wearos.presentation.navigation

sealed class WearScreen(val route: String) {
    data object Home : WearScreen("home")
    data object Chat : WearScreen("chat")
    data object QuickActions : WearScreen("quick_actions")
    data object RecentChats : WearScreen("recent_chats")
    data object Notifications : WearScreen("notifications")
    data object CronFeed : WearScreen("cron_feed")
    data object Settings : WearScreen("settings")
}
