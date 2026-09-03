package com.hermes.wearos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hermes.wearos.presentation.screens.AssistantScreen
import com.hermes.wearos.presentation.screens.SettingsScreen
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

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearScreen.Home.route
    ) {
        // ── Main Gemini-Style Assistant Hub ────────────────────────
        composable(WearScreen.Home.route) {
            AssistantScreen(
                chatViewModel = chatViewModel,
                voiceViewModel = voiceViewModel,
                onNavigateToSettings = {
                    navController.navigate(WearScreen.Settings.route)
                }
            )
        }

        // ── Settings ───────────────────────────────────────────────
        composable(WearScreen.Settings.route) {
            SettingsScreen(
                mainViewModel = mainViewModel,
                chatViewModel = chatViewModel,
                voiceViewModel = voiceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
