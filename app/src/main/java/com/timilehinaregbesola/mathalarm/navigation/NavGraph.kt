package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.navigation.NavGraph.ANIM_TRANSITION_DURATION
import com.timilehinaregbesola.mathalarm.presentation.MainActivity.Companion.deeplinkInfo
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
import kotlinx.serialization.json.Json

@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun NavGraph(preferences: AlarmPreferencesImpl) {
    val backStack = rememberNavBackStack(AlarmList)
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }

    val currentDeeplinkInfo = rememberUpdatedState(deeplinkInfo)
    LaunchedEffect(currentDeeplinkInfo) {
        currentDeeplinkInfo.value?.let {
            backStack.add(AlarmMath(it, false))
        }
    }

    NavDisplay(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategy = bottomSheetStrategy,
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            slideInHorizontally(
                animationSpec = tween(ANIM_TRANSITION_DURATION),
                initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(ANIM_TRANSITION_DURATION),
                        targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(ANIM_TRANSITION_DURATION),
                initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(ANIM_TRANSITION_DURATION),
                        targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(ANIM_TRANSITION_DURATION),
                initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(ANIM_TRANSITION_DURATION),
                        targetOffsetX = { it })
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
                val alarmObject = Json.decodeFromString<AlarmEntity>(it.settingsAlarm)
                AlarmBottomSheet(
                    backstack = backStack,
                    darkTheme = preferences.shouldUseDarkColors(),
                    alarm = alarmObject
                )
            }

            entry<AlarmMath> {
                val alarmObject = Json.decodeFromString<AlarmEntity>(it.alarmJson)
                MathScreen(
                    backStack = backStack,
                    alarm = alarmObject,
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

private object NavGraph {
    val ANIM_TRANSITION_DURATION = 700
}
