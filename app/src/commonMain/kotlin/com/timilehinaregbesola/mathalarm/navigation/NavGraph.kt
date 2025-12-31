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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.navigation.NavGraph.ANIM_TRANSITION_DURATION
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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun NavGraph(
    preferences: AlarmPreferencesImpl,
    deeplinkInfo: String?
) {
    val config = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(AlarmList::class, AlarmList.serializer())
                subclass(AppSettings::class, AppSettings.serializer())
                subclass(AlarmMath::class, AlarmMath.serializer())
                subclass(SettingsSheet::class, SettingsSheet.serializer())
            }
        }
    }
    val backStack = rememberNavBackStack(config, AlarmList)
    val bottomSheetStrategy = remember {
        BottomSheetSceneStrategy<NavKey>()
    }

    // Navigate to MathScreen when deeplinkInfo changes (e.g., from notification tap)
    LaunchedEffect(deeplinkInfo) {
        println("NavGraph: LaunchedEffect triggered with deeplinkInfo = $deeplinkInfo")
        deeplinkInfo?.let {
            println("NavGraph: Navigating to AlarmMath")
            backStack.add(AlarmMath(it, false))
        }
    }

    NavDisplay(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = {
            // Block back navigation when on AlarmMath screen - user must solve or snooze
            if (backStack.lastOrNull() !is AlarmMath && backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        sceneStrategy = bottomSheetStrategy,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
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
                    onBackPress = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    pref = preferences
                )
            }

        }
    )
}

private object NavGraph {
    val ANIM_TRANSITION_DURATION = 700
}
