package com.bodyswitch.checkin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.SessionManager
import com.bodyswitch.checkin.ui.checkin.CheckinCompleteScreen
import com.bodyswitch.checkin.ui.checkin.CheckinScreen
import com.bodyswitch.checkin.ui.history.CheckinHistoryScreen
import com.bodyswitch.checkin.ui.home.MainCheckinScreen
import com.bodyswitch.checkin.ui.login.LoginScreen
import com.bodyswitch.checkin.ui.settings.SettingsScreen
import java.net.URLEncoder

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHECKIN_QR = "checkin_qr/{qrData}"
    const val CHECKIN_TOKEN = "checkin_token/{token}"
    const val CHECKIN_COMPLETE = "checkin_complete"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun checkinQr(qrData: String): String {
        val encoded = URLEncoder.encode(qrData, "UTF-8")
        return "checkin_qr/$encoded"
    }

    fun checkinToken(token: String): String {
        val encoded = URLEncoder.encode(token, "UTF-8")
        return "checkin_token/$encoded"
    }
}

@Composable
fun NavGraph(sessionManager: SessionManager, checkinSettingsManager: CheckinSettingsManager) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            MainCheckinScreen(
                sessionManager = sessionManager,
                checkinSettingsManager = checkinSettingsManager,
                onQrScanned = { qrData ->
                    navController.navigate(Routes.checkinQr(qrData)) {
                        launchSingleTop = true
                    }
                },
                onPhoneLogin = { token ->
                    navController.navigate(Routes.checkinToken(token)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onLogout = {
                    sessionManager.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.CHECKIN_QR,
            arguments = listOf(navArgument("qrData") { type = NavType.StringType }),
        ) {
            CheckinScreen(
                onBack = { navController.popBackStack() },
                onCheckinComplete = {
                    navController.navigate(Routes.CHECKIN_COMPLETE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }

        composable(
            route = Routes.CHECKIN_TOKEN,
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
        ) {
            CheckinScreen(
                onBack = { navController.popBackStack() },
                onCheckinComplete = {
                    navController.navigate(Routes.CHECKIN_COMPLETE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }

        composable(Routes.HISTORY) {
            CheckinHistoryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                checkinSettingsManager = checkinSettingsManager,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CHECKIN_COMPLETE) {
            CheckinCompleteScreen(
                onScanAgain = { navController.popBackStack(Routes.HOME, inclusive = false) },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }
    }
}
