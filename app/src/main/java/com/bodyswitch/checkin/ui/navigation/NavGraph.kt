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
import com.bodyswitch.checkin.ui.checkin.EmployeeAttendTypeScreen
import com.bodyswitch.checkin.ui.checkin.EmployeeCheckinCompleteScreen
import java.net.URLDecoder
import com.bodyswitch.checkin.ui.history.CheckinHistoryScreen
import com.bodyswitch.checkin.ui.home.MainCheckinScreen
import com.bodyswitch.checkin.ui.login.LoginScreen
import com.bodyswitch.checkin.ui.settings.SettingsScreen
import com.bodyswitch.checkin.ui.splash.SplashScreen
import java.net.URLEncoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHECKIN_QR = "checkin_qr/{qrData}"
    const val CHECKIN_TOKEN = "checkin_token/{token}"
    const val CHECKIN_COMPLETE = "checkin_complete"
    const val EMPLOYEE_ATTEND_TYPE = "employee_attend_type"
    const val EMPLOYEE_CHECKIN_COMPLETE = "employee_checkin_complete/{name}/{time}/{count}/{exitCount}/{attendType}"
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

    fun employeeCheckinComplete(name: String, time: String?, count: Int?, exitCount: Int?, attendType: String = "ENTRY"): String {
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val encodedTime = URLEncoder.encode(time ?: "", "UTF-8")
        val encodedCount = (count ?: 0).toString()
        val encodedExitCount = (exitCount ?: 0).toString()
        return "employee_checkin_complete/$encodedName/$encodedTime/$encodedCount/$encodedExitCount/$attendType"
    }
}

@Composable
fun NavGraph(sessionManager: SessionManager, checkinSettingsManager: CheckinSettingsManager) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

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
                onEmployeeAttendType = {
                    navController.navigate(Routes.EMPLOYEE_ATTEND_TYPE) {
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
                onEmployeeAttendType = {
                    navController.navigate(Routes.EMPLOYEE_ATTEND_TYPE) {
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
                onEmployeeAttendType = {
                    navController.navigate(Routes.EMPLOYEE_ATTEND_TYPE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }

        composable(Routes.HISTORY) {
            CheckinHistoryScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                checkinSettingsManager = checkinSettingsManager,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.CHECKIN_COMPLETE) {
            CheckinCompleteScreen(
                onScanAgain = { navController.popBackStack(Routes.HOME, inclusive = false) },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }

        composable(Routes.EMPLOYEE_ATTEND_TYPE) {
            EmployeeAttendTypeScreen(
                onComplete = { name, time, count, exitCount, attendType ->
                    navController.navigate(Routes.employeeCheckinComplete(name, time, count, exitCount, attendType)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }

        composable(
            route = Routes.EMPLOYEE_CHECKIN_COMPLETE,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("time") { type = NavType.StringType },
                navArgument("count") { type = NavType.IntType },
                navArgument("exitCount") { type = NavType.IntType },
                navArgument("attendType") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
            val time = URLDecoder.decode(backStackEntry.arguments?.getString("time") ?: "", "UTF-8")
                .ifEmpty { null }
            val count = backStackEntry.arguments?.getInt("count") ?: 0
            val exitCount = backStackEntry.arguments?.getInt("exitCount") ?: 0
            val attendType = backStackEntry.arguments?.getString("attendType") ?: "ENTRY"

            EmployeeCheckinCompleteScreen(
                onScanAgain = { navController.popBackStack(Routes.HOME, inclusive = false) },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
                employeeName = name,
                checkinTime = time,
                entryCount = if (count > 0) count else null,
                exitCount = if (exitCount > 0) exitCount else null,
                attendType = attendType,
            )
        }
    }
}
