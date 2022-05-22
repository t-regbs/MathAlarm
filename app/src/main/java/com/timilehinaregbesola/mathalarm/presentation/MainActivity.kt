package com.timilehinaregbesola.mathalarm.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterialNavigationApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = rememberIsDarkTheme(viewModel)
            updateTheme(isDarkTheme)
            MathAlarmTheme(
                darkTheme = isDarkTheme
            ) {
                NavGraph(isDarkTheme)
            }
        }
    }

    private fun updateTheme(darkTheme: Boolean) {
        window.apply {
            statusBarColor = if (darkTheme) Color.BLACK else Color.WHITE
            navigationBarColor = if (darkTheme) Color.BLACK else Color.WHITE
            WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars =
                !darkTheme
        }
    }

    @Composable
    private fun rememberIsDarkTheme(viewModel: MainViewModel): Boolean {
        val isSystemDarkTheme = isSystemInDarkTheme()

        val theme by remember(viewModel) {
            viewModel.loadCurrentTheme()
        }.collectAsState(initial = AppThemeOptions.SYSTEM)

        val isDarkTheme = when (theme) {
            AppThemeOptions.LIGHT -> false
            AppThemeOptions.DARK -> true
            AppThemeOptions.SYSTEM -> isSystemDarkTheme
        }
        return isDarkTheme
    }
}
