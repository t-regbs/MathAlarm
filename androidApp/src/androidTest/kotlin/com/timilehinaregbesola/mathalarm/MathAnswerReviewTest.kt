package com.timilehinaregbesola.mathalarm

import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmList
import com.timilehinaregbesola.mathalarm.utils.Destinations.SettingsSheet
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * Regressions for the two review-related UI failures reproduced before the fix.
 * MainActivity already installs content, so these tests replace it through Activity.setContent
 * on the UI thread rather than ComposeUiTest.setContent. Cleanup also runs on the UI thread
 * to serialize ViewModel disposal with its UI callbacks.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class, InternalCoroutinesApi::class)
class MathAnswerReviewTest {
    @Test fun correctAnswerAdvancesAndClearsPreviousIncorrectMessage() = runAndroidComposeUiTest<MainActivity> {
        val vm = GlobalContext.get().get<AlarmMathViewModel>()
        val host = requireNotNull(activity)
        runOnUiThread {
            host.setContent {
                CompositionLocalProvider(LocalStrings provides EnMathAlarmStrings) {
                    MathAlarmTheme(darkTheme = false) {
                        MathScreen(NavBackStack<NavKey>(AlarmList, SettingsSheet("")),
                            AlarmMapper().mapFromDomainModel(Alarm(questionCount = 2, vibrate = false)),
                            viewModel = vm, fromSheet = true)
                    }
                }
            }
        }
        try {
            waitUntil(timeoutMillis = 5_000) { vm.currentProblem != null }
            val problem = vm.currentProblem!!
            val expected = when (problem.operator) {
                MathProblemOperator.Add -> problem.numOne + problem.numTwo
                MathProblemOperator.Subtract -> problem.numOne - problem.numTwo
                MathProblemOperator.Times -> problem.numOne * problem.numTwo
                MathProblemOperator.Divide -> problem.numOne / problem.numTwo
            }
            onNode(hasSetTextAction()).performTextReplacement((expected + 1).toString())
            onNodeWithText(EnMathAlarmStrings.checkAnswer).performClick()
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertIsDisplayed()
            onNode(hasSetTextAction()).performTextReplacement(expected.toString())
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertDoesNotExist()
            onNodeWithText(EnMathAlarmStrings.checkAnswer).performClick()
            runOnIdle { assertEquals(1, vm.state.value.questionIndex) }
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertDoesNotExist()
        } finally { runOnUiThread { vm.close() } }
    }

    @Test fun integerWrittenAsDecimalIsPreservedAndAccepted() = runAndroidComposeUiTest<MainActivity> {
        val vm = GlobalContext.get().get<AlarmMathViewModel>()
        val host = requireNotNull(activity)
        runOnUiThread {
            host.setContent {
                CompositionLocalProvider(LocalStrings provides EnMathAlarmStrings) {
                    MathAlarmTheme(darkTheme = false) {
                        MathScreen(NavBackStack<NavKey>(AlarmList, SettingsSheet("")),
                            AlarmMapper().mapFromDomainModel(Alarm(questionCount = 2, vibrate = false,
                                difficulty = 3, challengeOperations = "+")), viewModel = vm, fromSheet = true)
                    }
                }
            }
        }
        try {
            waitUntil(timeoutMillis = 5_000) { vm.currentProblem != null }
            val problem = vm.currentProblem!!
            val expected = problem.numOne + problem.numTwo
            onNode(hasSetTextAction()).performTextReplacement("$expected.5")
            runOnIdle { assertEquals("$expected.5", vm.state.value.answerText) }
            onNodeWithText(EnMathAlarmStrings.checkAnswer).performClick()
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertIsDisplayed()
            runOnIdle { assertEquals(0, vm.state.value.questionIndex) }
            onNode(hasSetTextAction()).performTextReplacement("$expected.0")
            runOnIdle { assertEquals("$expected.0", vm.state.value.answerText) }
            onNodeWithText(EnMathAlarmStrings.checkAnswer).performClick()
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertDoesNotExist()
            runOnIdle { assertEquals(1, vm.state.value.questionIndex) }
        } finally { runOnUiThread { vm.close() } }
    }
    @Test fun completionDoesNotWaitForPreviousErrorSnackbar() = runAndroidComposeUiTest<MainActivity> {
        val vm = GlobalContext.get().get<AlarmMathViewModel>()
        val host = requireNotNull(activity)
        val navigation = NavBackStack<NavKey>(AlarmList, SettingsSheet(""))
        runOnUiThread {
            host.setContent {
                CompositionLocalProvider(LocalStrings provides EnMathAlarmStrings) {
                    MathAlarmTheme(darkTheme = false) {
                        MathScreen(navigation,
                            AlarmMapper().mapFromDomainModel(Alarm(questionCount = 1, vibrate = false,
                                difficulty = 3, challengeOperations = "+")), viewModel = vm, fromSheet = true)
                    }
                }
            }
        }
        try {
            waitUntil(timeoutMillis = 5_000) { vm.currentProblem != null }
            val problem = vm.currentProblem!!
            val expected = problem.numOne + problem.numTwo
            onNode(hasSetTextAction()).performTextReplacement((expected + 1).toString())
            onNodeWithText(EnMathAlarmStrings.checkAnswer).performClick()
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertIsDisplayed()
            onNode(hasSetTextAction()).performTextReplacement(expected.toString())
            onNodeWithText(EnMathAlarmStrings.checkAnswer).performClick()
            runOnIdle { assertEquals(1, navigation.size) }
            onNodeWithText(EnMathAlarmStrings.incorrectAnswer).assertDoesNotExist()
        } finally { runOnUiThread { vm.close() } }
    }

}
