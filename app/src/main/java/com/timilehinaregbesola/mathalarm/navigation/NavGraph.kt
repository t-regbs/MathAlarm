package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListScreen
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreen

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NavGraph(
    viewModel: AlarmListViewModel
) {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colors.background) {
        NavHost(navController = navController, startDestination = "home_screen") {
            composable("home_screen") {
                AlarmListScreen(navController, viewModel)
            }
            composable("math_screen") { MathScreen(navController = navController) }
        }
    }
}
