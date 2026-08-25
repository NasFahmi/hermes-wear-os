package com.hermes.wearos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hermes.wearos.presentation.screens.*
import com.hermes.wearos.presentation.viewmodel.ChatViewModel
import com.hermes.wearos.presentation.viewmodel.MainViewModel
import com.hermes.wearos.presentation.viewmodel.VoiceViewModel

@Composable
fun WearNavHost(
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    voiceViewModel: VoiceViewModel
) {
    val navController = rememberSwipeDismissableNavController()
    val messages by chatViewModel.messages.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val cronFeed by mainViewModel.cronFeed.collectAsState()

    var chatInputMode by remember { mutableStateOf(ChatInputMode.NONE) }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearScreen.Home.route
    ) {
        // ── Home (Minimalist Ask & Answer Hub) ──────────────────────
        composable(WearScreen.Home.route) {
            HomeScreen(
                mainViewModel = mainViewModel,
                onNavigateToChat = { mode ->
                    chatInputMode = mode
                    navController.navigate(WearScreen.Chat.route)
                },
                onNavigateToSettings = {
                    navController.navigate(WearScreen.Settings.route)
                }
            )
        }

        // ── Chat (Q&A with Voice STT & Keyboard Input) ─────────────
        composable(WearScreen.Chat.route) {
            ChatScreen(
                chatViewModel = chatViewModel,
                voiceViewModel = voiceViewModel,
                initialMode = chatInputMode,
                onBack = {
                    chatInputMode = ChatInputMode.NONE
                    navController.popBackStack()
                }
            )
        }

        // ── Settings (Configuration, Server Status & Submenus) ──────
        composable(WearScreen.Settings.route) {
            SettingsScreen(
                mainViewModel = mainViewModel,
                chatViewModel = chatViewModel,
                voiceViewModel = voiceViewModel,
                onNavigateToNotifications = {
                    navController.navigate(WearScreen.Notifications.route)
                },
                onNavigateToCron = {
                    navController.navigate(WearScreen.CronFeed.route)
                },
                onNavigateToRecent = {
                    navController.navigate(WearScreen.RecentChats.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Recent Chats (Riwayat Chat dari Settings) ──────────────
        composable(WearScreen.RecentChats.route) {
            RecentChatsScreen(
                messages = messages,
                onSelectMessage = { text ->
                    chatViewModel.sendMessage(text)
                    chatInputMode = ChatInputMode.NONE
                    navController.navigate(WearScreen.Chat.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Notifications (Notifikasi dari Settings) ────────────────
        composable(WearScreen.Notifications.route) {
            NotificationsScreen(
                notifications = notifications,
                onDismiss = { navController.popBackStack() },
                onNotificationTap = { notif ->
                    mainViewModel.dismissNotification(notif.id)
                }
            )
        }

        // ── Cron Feed (Cron Pantauan dari Settings) ─────────────────
        composable(WearScreen.CronFeed.route) {
            CronFeedScreen(
                cronItems = cronFeed,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
