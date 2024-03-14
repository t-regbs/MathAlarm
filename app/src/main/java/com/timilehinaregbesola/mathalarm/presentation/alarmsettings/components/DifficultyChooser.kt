package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DifficultyChooser(initialDiff: Int, onValueChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf("Easy Math", "Medium Math", "Hard Math")
    Box {
        Text(
            items[initialDiff],
            modifier = Modifier
                .clickable(onClick = { expanded = true }),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEachIndexed { index, s ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onValueChange(index)
                    },
                    text = { Text(text = s) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun DifficultyChooserPreview() {
    MaterialTheme {
        DifficultyChooser(initialDiff = 1) {}
    }
}
