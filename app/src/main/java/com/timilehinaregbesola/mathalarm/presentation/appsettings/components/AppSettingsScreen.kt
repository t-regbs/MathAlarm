package com.timilehinaregbesola.mathalarm.presentation.appsettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun AppSettingsScreen(
    onBackPress: () -> Unit,
    pref: AlarmPreferencesImpl
) {
    val isDark = when (pref.theme) {
        Theme.DARK -> true
        Theme.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    val options = listOf(
        Triple("Light", Icons.Filled.WbSunny, Theme.LIGHT),
        Triple("Dark", Icons.Filled.DarkMode, Theme.DARK),
        Triple("Default", Icons.Filled.Smartphone, Theme.SYSTEM)
    )
    var selectedOption by remember { mutableStateOf(pref.theme) }
    val onSelectionChange = { newTheme: Theme ->
        selectedOption = newTheme
        pref.theme = newTheme
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "App Settings",
                            fontSize = 16.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
                            onClick = onBackPress
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                )
            },
        ) {
            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                Text(
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                    text = "Color Theme"
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isDark) {
                                    MaterialTheme.colors.primaryVariant
                                } else {
                                    Color.LightGray
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(MaterialTheme.spacing.extraSmall)
                    ) {
                        Row {
                            options.forEach { triple ->
                                Row(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clip(shape = RoundedCornerShape(size = 16.dp))
                                        .clickable {
                                            onSelectionChange(triple.third)
                                        }
                                        .background(
                                            if (triple.third == selectedOption) {
                                                MaterialTheme.colors.primary
                                            } else {
                                                if (isDark) {
                                                    MaterialTheme.colors.primaryVariant
                                                } else {
                                                    Color.LightGray
                                                }
                                            }
                                        )
                                        .padding(
                                            vertical = MaterialTheme.spacing.extraSmall,
                                        ),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(end = 2.dp),
                                        imageVector = triple.second,
                                        contentDescription = triple.first
                                    )
                                    if (triple.third == selectedOption) {
                                        Text(
                                            text = triple.first,
                                            style = typography.body1.merge(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
