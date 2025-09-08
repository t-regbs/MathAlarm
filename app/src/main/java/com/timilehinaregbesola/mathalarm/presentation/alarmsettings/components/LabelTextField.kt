package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelTextField(
    text: TextFieldValue,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    onValueChange: (TextFieldValue) -> Unit,
) {
    TextField(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.spacing.medium)
            .fillMaxWidth(),
        value = text,
        onValueChange = onValueChange,
        leadingIcon = { Icon(imageVector = Icons.Outlined.Label, contentDescription = null) },
        label = label,
        placeholder = placeholder,
        singleLine = true,
        colors = colors(
            unfocusedContainerColor = Transparent,
            focusedContainerColor = Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Preview
@Composable
private fun LabelTextFieldPreview() {
    MaterialTheme {
        LabelTextField(text = TextFieldValue("FieldPreview"), onValueChange = {})
    }
}
