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
import androidx.navigation.plusAssign
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.timilehinaregbesola.mathalarm.presentation.NavGraphs
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalMaterialNavigationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NavGraph(preferences: AlarmPreferencesImpl) {
    val pref = preferences
    val navHostEngine = rememberAnimatedNavHostEngine()
    val navController = rememberAnimatedNavController()
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    navController.navigatorProvider += bottomSheetNavigator

    Surface(color = MaterialTheme.colors.background) {
        ModalBottomSheetLayout(
            bottomSheetNavigator = bottomSheetNavigator,
            sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        ) {
            DestinationsNavHost(
                dependenciesContainerBuilder = { // this: DependenciesContainerBuilder<*>
                    dependency(pref)
                },
                engine = navHostEngine,
                navController = navController,
                navGraph = NavGraphs.root
            )
//            NavHost(navController = navController, startDestination = Navigation.NAV_ALARM_LIST) {
//                composable(Navigation.NAV_ALARM_LIST) {
//                    ListDisplayScreen(
//                        onNavigate = { navController.navigate(it.route) },
//                        navController = navController,
//                        darkTheme = preferences.shouldUseDarkColors()
//                    )
//                }
//                composable(
//                    route = Navigation.NAV_ALARM_MATH,
//                    arguments = listOf(
//                        navArgument(Navigation.NAV_ALARM_MATH_ARGUMENT) {
//                            type = NavType.LongType
//                        }
//                    ),
//                    deepLinks = listOf(navDeepLink { uriPattern = Navigation.NAV_ALARM_MATH_URI })
//                ) {
//                    MathScreen(
//                        navController = navController,
//                        alarmId = it.getAlarmIdArgument(Navigation.NAV_ALARM_MATH_ARGUMENT),
//                        darkTheme = preferences.shouldUseDarkColors()
//                    )
//                }
//                composable(Navigation.NAV_APP_SETTINGS) {
//                    AppSettingsScreen(
//                        onBackPress = { navController.popBackStack() },
//                        pref = preferences
//                    )
//                }
//                bottomSheet(
//                    route = Navigation.NAV_SETTINGS_SHEET,
//                    arguments = listOf(
//                        navArgument(Navigation.NAV_SETTINGS_SHEET_ARGUMENT) {
//                            type = NavType.LongType
//                            defaultValue = -1L
//                        }
//                    )
//                ) {
//                    AlarmBottomSheet(
//                        navController = navController,
//                        darkTheme = preferences.shouldUseDarkColors()
//                    )
//                }
//            }
        }
    }
}
