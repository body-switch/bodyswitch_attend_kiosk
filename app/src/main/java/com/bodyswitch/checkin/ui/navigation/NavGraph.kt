package com.bodyswitch.checkin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bodyswitch.checkin.data.session.CheckinSettingsManager
import com.bodyswitch.checkin.data.session.SessionExpiryViewModel
import com.bodyswitch.checkin.data.session.SessionManager
import com.bodyswitch.checkin.ui.access.AccessRegistrationScreen
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
    // 선택 인자가 붙은 실제 등록 경로. popUpTo 는 이 패턴으로 잡아야 한다.
    const val LOGIN_PATTERN = "login?expired={expired}"
    const val LOGIN_EXPIRED = "login?expired=true"
    const val HOME = "home"
    const val CHECKIN_QR = "checkin_qr/{qrData}"
    const val CHECKIN_TOKEN = "checkin_token/{token}"
    const val CHECKIN_COMPLETE = "checkin_complete"
    const val CHECKOUT_COMPLETE = "checkout_complete"
    const val EMPLOYEE_ATTEND_TYPE = "employee_attend_type"
    const val EMPLOYEE_CHECKIN_COMPLETE = "employee_checkin_complete/{name}/{time}/{count}/{exitCount}/{attendType}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ACCESS_REGISTRATION = "access_registration"

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
fun NavGraph(
    sessionManager: SessionManager,
    checkinSettingsManager: CheckinSettingsManager,
    sessionExpiryViewModel: SessionExpiryViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    // 관리자 토큰 만료가 확정되면 어느 화면에 있든 로그인으로 보낸다. 안내 배너는 로그인 화면이 띄운다.
    LaunchedEffect(Unit) {
        sessionExpiryViewModel.expired.collect {
            navController.navigate(Routes.LOGIN_EXPIRED) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                // 세션은 영속화되므로 프로세스가 회수됐다 다시 떠도 로그인 상태면 홈으로 바로 간다.
                onSplashFinished = {
                    val next = if (sessionManager.isLoggedIn) Routes.HOME else Routes.LOGIN
                    navController.navigate(next) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.LOGIN_PATTERN,
            arguments = listOf(navArgument("expired") { type = NavType.BoolType; defaultValue = false }),
        ) { backStackEntry ->
            LoginScreen(
                sessionExpired = backStackEntry.arguments?.getBoolean("expired") ?: false,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN_PATTERN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            // 홈에 들어올 때마다 토큰 만료를 확인한다. 이후는 10분 주기.
            LaunchedEffect(Unit) { sessionExpiryViewModel.checkNow() }
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
                onAccessRegistration = {
                    navController.navigate(Routes.ACCESS_REGISTRATION) {
                        launchSingleTop = true
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
                onCheckoutComplete = {
                    navController.navigate(Routes.CHECKOUT_COMPLETE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onEmployeeAttendType = {
                    navController.navigate(Routes.EMPLOYEE_ATTEND_TYPE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onRequireLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
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
                onCheckoutComplete = {
                    navController.navigate(Routes.CHECKOUT_COMPLETE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onEmployeeAttendType = {
                    navController.navigate(Routes.EMPLOYEE_ATTEND_TYPE) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onRequireLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
            )
        }

        composable(Routes.ACCESS_REGISTRATION) {
            AccessRegistrationScreen(
                sessionManager = sessionManager,
                checkinSettingsManager = checkinSettingsManager,
                onExit = { navController.popBackStack(Routes.HOME, inclusive = false) },
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

        composable(Routes.CHECKOUT_COMPLETE) {
            CheckinCompleteScreen(
                onScanAgain = { navController.popBackStack(Routes.HOME, inclusive = false) },
                centerName = sessionManager.businessName ?: sessionManager.branchName ?: "",
                title = "퇴실 완료",
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
