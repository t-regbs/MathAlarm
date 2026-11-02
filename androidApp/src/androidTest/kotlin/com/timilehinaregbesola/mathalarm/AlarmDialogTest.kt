package com.timilehinaregbesola.mathalarm

import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.DialogArguments
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.MathAlarmDialog
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    InternalCoroutinesApi::class,
)
class AlarmDialogTest {
    @Test
    fun confirmationUsesOneDialogAndKeepsActionsSeparate() = runAndroidComposeUiTest<MainActivity> {
        var confirmations = 0
        var cancellations = 0
        runOnUiThread {
            requireNotNull(activity).setContent {
                MathAlarmTheme(darkTheme = false) {
                    MathAlarmDialog(
                        arguments = DialogArguments(
                            title = "Clear alarms",
                            text = "Remove saved alarms?",
                            confirmText = "Clear",
                            dismissText = "Cancel",
                            onConfirmAction = { confirmations++ },
                        ),
                        isDialogOpen = true,
                        onDismissRequest = { cancellations++ },
                    )
                }
            }
        }
        onAllNodes(isDialog()).assertCountEquals(1)
        onNodeWithText("Cancel").performClick()
        runOnIdle {
            assertEquals(0, confirmations)
            assertEquals(1, cancellations)
        }
        onNodeWithText("Clear").performClick()
        runOnIdle {
            assertEquals(1, confirmations)
            assertEquals(1, cancellations)
        }
    }
}
