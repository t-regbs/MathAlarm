package com.timilehinaregbesola.mathalarm.presentation.components

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.R

@Composable
fun ListTopAppBar(
    openDialog: MutableState<Boolean>
) {
    TopAppBar(
        title = {
            Text(
                text = "Alarms",
                fontSize = 16.sp
            )
        },
        backgroundColor = Color.White,
        actions = {
            IconButton(onClick = { openDialog.value = true }) {
                Icon(painterResource(id = R.drawable.outline_delete_sweep_24), contentDescription = null)
            }
            IconButton(onClick = { }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
            }
        }
    )
}
