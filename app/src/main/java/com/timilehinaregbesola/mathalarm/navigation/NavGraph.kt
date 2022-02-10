package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListDisplayScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.getAlarmIdArgument
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(ExperimentalMaterialNavigationApi::class)
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NavGraph(preferences: AlarmPreferencesImpl) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    Surface(color = MaterialTheme.colors.background) {
        ModalBottomSheetLayout(
            bottomSheetNavigator = bottomSheetNavigator,
            sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        ) {
            NavHost(navController = navController, startDestination = Navigation.NAV_ALARM_LIST) {
                composable(Navigation.NAV_ALARM_LIST) {
                    ListDisplayScreen(
                        onNavigate = { navController.navigate(it.route) },
                        navController = navController,
                        darkTheme = preferences.shouldUseDarkColors()
                    )
                }
                composable(
                    route = Navigation.NAV_ALARM_MATH,
                    arguments = listOf(
                        navArgument(Navigation.NAV_ALARM_MATH_ARGUMENT) {
                            type = NavType.LongType
                        }
                    ),
                    deepLinks = listOf(navDeepLink { uriPattern = Navigation.NAV_ALARM_MATH_URI })
                ) {
                    MathScreen(
                        navController = navController,
                        alarmId = it.getAlarmIdArgument(Navigation.NAV_ALARM_MATH_ARGUMENT),
                        darkTheme = preferences.shouldUseDarkColors()
                    )
                }
                composable(Navigation.NAV_APP_SETTINGS) {
                    AppSettingsScreen(
                        onBackPress = { navController.popBackStack() },
                        pref = preferences
                    )
                }
                bottomSheet(
                    route = Navigation.NAV_SETTINGS_SHEET,
                    arguments = listOf(
                        navArgument(Navigation.NAV_SETTINGS_SHEET_ARGUMENT) {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) {
                    AlarmBottomSheet(
                        navController = navController,
                        darkTheme = preferences.shouldUseDarkColors()
                    )
                }
            }
        }
    }
}
