package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation3.scene.Scene
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.whatsnew.AnnouncementFeature
import com.timilehinaregbesola.mathalarm.presentation.whatsnew.announcementFeaturesToShow
import com.timilehinaregbesola.mathalarm.presentation.whatsnew.WhatsNewDialog
import com.timilehinaregbesola.mathalarm.presentation.whatsnew.announcementCatalog
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
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalFoundationApi
@Composable
fun NavGraph(
    preferences: AlarmPreferencesImpl,
    deeplinkInfo: String?,
    onDeeplinkConsumed: () -> Unit = {}
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
    val catalog = announcementCatalog
    // Freeze the pages for this session: acknowledging one must not remove it mid-navigation.
    var announcementIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var automaticAnnouncementOffered by rememberSaveable { mutableStateOf(false) }
    val settingsLayout = settingsWindowLayout(currentWindowAdaptiveInfo().windowSizeClass)
    val bottomSheetStrategy = remember(settingsLayout.useCenteredDialog) {
        BottomSheetSceneStrategy<NavKey>(useCenteredDialog = settingsLayout.useCenteredDialog)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val mathPreviewStrategy = remember { MathPreviewSceneStrategy<NavKey>() }

    // Navigate to MathScreen when deeplinkInfo changes (e.g., from notification tap)
    LaunchedEffect(deeplinkInfo) {
        println("NavGraph: LaunchedEffect triggered with deeplinkInfo = $deeplinkInfo")
        deeplinkInfo?.let {
            println("NavGraph: Navigating to AlarmMath")
            backStack.add(AlarmMath(it, false))
            onDeeplinkConsumed()
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
        sceneStrategies = listOf(mathPreviewStrategy, bottomSheetStrategy, listDetailStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            sheetTransition(initialState, targetState) ?: (slideInHorizontally(
                animationSpec = tween(ANIM_TRANSITION_DURATION),
                initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(ANIM_TRANSITION_DURATION),
                        targetOffsetX = { -it }))
        },
        popTransitionSpec = {
            sheetTransition(initialState, targetState) ?: (slideInHorizontally(
                animationSpec = tween(ANIM_TRANSITION_DURATION),
                initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(ANIM_TRANSITION_DURATION),
                        targetOffsetX = { it }))
        },
        predictivePopTransitionSpec = {
            sheetTransition(initialState, targetState) ?: (slideInHorizontally(
                animationSpec = tween(ANIM_TRANSITION_DURATION),
                initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(ANIM_TRANSITION_DURATION),
                        targetOffsetX = { it }))
        },
        entryProvider = entryProvider {
            entry<AlarmList>(
                metadata = ListDetailSceneStrategy.listPane(sceneKey = AlarmList)
            ) {
                ListDisplayScreen(
                    backstack = backStack,
                    darkTheme = preferences.shouldUseDarkColors(),
                )
            }

            entry<SettingsSheet>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = AlarmList) +
                    BottomSheetSceneStrategy.bottomSheet()
            ) {
                val alarmObject = Json.decodeFromString<AlarmEntity>(it.settingsAlarm)
                AlarmBottomSheet(
                    backstack = backStack,
                    darkTheme = preferences.shouldUseDarkColors(),
                    alarm = alarmObject,
                    showDismissButton = settingsLayout.showDismissButton,
                )
            }

            entry<AlarmMath>(
                metadata = { destination ->
                    mapOf("mathScreen" to true) +
                        if (destination.fromSheet) MathPreviewSceneStrategy.metadata() else emptyMap()
                },
            ) {
                val alarmObject = Json.decodeFromString<AlarmEntity>(it.alarmJson)
                MathScreen(
                    backStack = backStack,
                    alarm = alarmObject,
                    fromSheet = it.fromSheet,
                )
            }

            entry<AppSettings> {
                AppSettingsScreen(
                    onBackPress = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    },
                    pref = preferences,
                    onWhatsNew = {
                        announcementIds = preferences.latestAnnouncementBatch(catalog.map { it.feature.id })
                    },
                )
            }

        }
    )
    val destination = backStack.lastOrNull()
    val canShowAnnouncement = deeplinkInfo == null &&
        (destination == AlarmList || destination == AppSettings)
    LaunchedEffect(canShowAnnouncement, destination) {
        if (canShowAnnouncement && destination == AlarmList && !automaticAnnouncementOffered) {
            automaticAnnouncementOffered = true
            preferences.latestAnnouncementBatch(catalog.map { it.feature.id })
            if (announcementIds == null) {
                announcementIds = announcementFeaturesToShow(preferences::hasSeenAnnouncement)
                    .map { it.id }.takeIf { it.isNotEmpty() }
            }
        }
    }
    val sessionAnnouncements = announcementIds?.mapNotNull { id ->
        catalog.find { it.feature.id == id }
    }.orEmpty()
    if (canShowAnnouncement && sessionAnnouncements.isNotEmpty()) {
        WhatsNewDialog(
            announcements = sessionAnnouncements,
            onSeen = preferences::markAnnouncementSeen,
            onDismiss = { announcementIds = null },
            onTryFeature = { feature ->
                announcementIds = null
                when (feature) {
                    AnnouncementFeature.MATH_CHALLENGES -> {
                        val alarmJson = Json.encodeToString(AlarmMapper().mapFromDomainModel(Alarm()))
                        backStack.add(SettingsSheet(alarmJson))
                    }
                    AnnouncementFeature.SKIP_NEXT -> {
                        while (backStack.size > 1) backStack.removeLastOrNull()
                    }
                }
            },
        )
    }
}

private object NavGraph {
    val ANIM_TRANSITION_DURATION = 700
}

// Modal sheets own their vertical motion; a page slide would move their backdrop too.
private fun sheetTransition(initialState: Scene<*>, targetState: Scene<*>): ContentTransform? {
    val entries = initialState.entries + targetState.entries
    return when {
        entries.any { it.metadata["mathScreen"] == true } ->
            fadeIn(tween(180)) togetherWith fadeOut(tween(180))
        initialState is BottomSheetScene<*> || targetState is BottomSheetScene<*> ->
            EnterTransition.None togetherWith ExitTransition.None
        else -> null
    }
}
