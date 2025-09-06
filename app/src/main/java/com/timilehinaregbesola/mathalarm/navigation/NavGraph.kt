package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListDisplayScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_ALARM_LIST
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_ALARM_MATH
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_ALARM_MATH_ARGUMENT
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_ALARM_MATH_URI
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_APP_SETTINGS

import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun NavGraph(preferences: AlarmPreferencesImpl) {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController = navController, startDestination = NAV_ALARM_LIST) {
            composable(
                route = NAV_ALARM_LIST,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(700),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(700),
                    )
                },
            ) {
                ListDisplayScreen(
                    navController = navController,
                    darkTheme = preferences.shouldUseDarkColors(),
                )
            }
            composable(
                route = NAV_ALARM_MATH,
                deepLinks = listOf(navDeepLink { uriPattern = NAV_ALARM_MATH_URI }),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(700),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(700),
                    )
                },
            ) {
                val alarmJson = it.arguments?.getString(NAV_ALARM_MATH_ARGUMENT)
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
                val alarmObject = alarmJson?.let { it1 -> jsonAdapter.fromJson(it1) }
                MathScreen(
                    navController = navController,
                    alarm = alarmObject!!,
                    darkTheme = preferences.shouldUseDarkColors(),
                )
            }
            composable(
                route = NAV_APP_SETTINGS,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(700),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(700),
                    )
                },
            ) {
                AppSettingsScreen(
                    onBackPress = { navController.popBackStack() },
                    pref = preferences,
                )
            }
        }
    }
}
