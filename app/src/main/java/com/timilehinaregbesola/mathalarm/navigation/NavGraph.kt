package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListDisplayScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.components.AppSettingsScreen
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmList
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
import com.timilehinaregbesola.mathalarm.utils.Destinations.AppSettings
import com.timilehinaregbesola.mathalarm.utils.Destinations.SettingsSheet
import kotlinx.coroutines.InternalCoroutinesApi

@OptIn(ExperimentalMaterialNavigationApi::class)
@ExperimentalMaterialNavigationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun NavGraph(preferences: AlarmPreferencesImpl) {
    Surface(color = MaterialTheme.colorScheme.background) {

        val backStack = rememberNavBackStack(AlarmList)
        val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategy = bottomSheetStrategy,
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = {
                slideInHorizontally(animationSpec = tween(700), initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { -it })
            },
            popTransitionSpec = {
                slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it })
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it })
            },
            entryProvider = entryProvider {
                entry<AlarmList> {
                    ListDisplayScreen(
                        backstack = backStack,
                        darkTheme = preferences.shouldUseDarkColors(),
                    )
                }

                entry<SettingsSheet>(
                    metadata = BottomSheetSceneStrategy.bottomSheet()
                ) {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
                    val alarmObject = jsonAdapter.fromJson(it.settingsAlarm)
                    AlarmBottomSheet(
                        backstack = backStack,
                        darkTheme = preferences.shouldUseDarkColors(),
                        alarm = alarmObject!!,
                    )
                }

                entry<AlarmMath> {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
                    val alarmObject = jsonAdapter.fromJson(it.alarmJson)
                    MathScreen(
                        backStack = backStack,
                        alarm = alarmObject!!,
                        darkTheme = preferences.shouldUseDarkColors(),
                        fromSheet = it.fromSheet
                    )
                }

                entry<AppSettings> {
                    AppSettingsScreen(
                        onBackPress = { backStack.removeLastOrNull() },
                        pref = preferences
                    )
                }

            }
        )
    }
}
