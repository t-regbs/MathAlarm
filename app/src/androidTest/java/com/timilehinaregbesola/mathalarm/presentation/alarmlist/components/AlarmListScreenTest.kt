package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.Navigation
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Rule

@ExperimentalMaterialNavigationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@HiltAndroidTest
// @UninstallModules(AppModule::class)
class AlarmListScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        composeRule.setContent {
            MathAlarmTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Navigation.NAV_ALARM_LIST) {
                    composable(Navigation.NAV_ALARM_LIST) {
                        ListDisplayScreen(
                            navController = navController,
                            darkTheme = false
                        )
                    }
                }
            }
        }
    }

    /*@Test
    fun clickSettingsDropdown_isVisible() {
        composeRule.onNodeWithTag(TestTags.SETTINGS_DROPDOWN).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("More").performClick()
        composeRule.onNodeWithTag(TestTags.SETTINGS_DROPDOWN).assertIsDisplayed()
    }*/
}
