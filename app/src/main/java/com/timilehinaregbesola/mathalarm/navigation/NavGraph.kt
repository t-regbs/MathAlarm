package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
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
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmBottomSheet
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.ListDisplayScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreen
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
fun NavGraph() {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    Surface(color = MaterialTheme.colors.background) {
        ModalBottomSheetLayout(bottomSheetNavigator) {
            NavHost(navController = navController, startDestination = Navigation.NAV_ALARM_LIST) {
                composable(Navigation.NAV_ALARM_LIST) {
                    ListDisplayScreen(
                        onNavigate = { navController.navigate(it.route) }
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
                        alarmId = it.getAlarmIdArgument(Navigation.NAV_ALARM_MATH_ARGUMENT)
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
                ) { backstackEntry ->
                    AlarmBottomSheet(
                        navController = navController,
                    )
                }
            }
        }
    }
}
