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
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreen
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.getAlarmIdArgument
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NavGraph(
    viewModel: AlarmListViewModel
) {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colors.background) {
        NavHost(navController = navController, startDestination = Navigation.NAV_ALARM_LIST) {
            composable(Navigation.NAV_ALARM_LIST) {
                AlarmListScreen(navController, viewModel)
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
                    viewModel = viewModel,
                    alarmId = it.getAlarmIdArgument(Navigation.NAV_ALARM_MATH_ARGUMENT)
                )
            }
        }
    }
}
