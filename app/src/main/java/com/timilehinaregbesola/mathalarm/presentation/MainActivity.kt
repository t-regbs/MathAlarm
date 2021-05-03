package com.timilehinaregbesola.mathalarm.presentation

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModel<AlarmListViewModel>()
    private var add: Boolean? = false

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.makeTransparentStatusBar()

//        var alarmId: Long? = null
//        val alarm = mainViewModel.alarm.value
//        mainViewModel.addClicked.observe(
//            this,
//            {
//                if (it != null) {
//                    add = it
//                }
//            }
//        )

        setContent {
            MathAlarmTheme {
                window.statusBarColor = MaterialTheme.colors.background.toArgb()
                NavGraph(mainViewModel)
            }
        }
    }
}

fun Window.makeTransparentStatusBar() {
    setFlags(
        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
    )
    setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    )
}
