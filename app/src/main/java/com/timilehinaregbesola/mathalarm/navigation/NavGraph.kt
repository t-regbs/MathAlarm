package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colors.background) {
        NavHost(navController = navController, startDestination = "welcome") {
            composable("alarm_list") { AlarmListScreen(navController = navController) }
            composable("math_screen") { Login(navController = navController) }
//            composable("home_screen") { HomeScreen(navController = navController) }
        }
    }
}