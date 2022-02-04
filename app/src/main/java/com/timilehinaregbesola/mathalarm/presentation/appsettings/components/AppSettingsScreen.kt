package com.timilehinaregbesola.mathalarm.presentation.appsettings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppSettingsScreen(
    onBackPress: () -> Unit
) {
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
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = onBackPress
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                )
            }
        ) {
        }
    }
}
