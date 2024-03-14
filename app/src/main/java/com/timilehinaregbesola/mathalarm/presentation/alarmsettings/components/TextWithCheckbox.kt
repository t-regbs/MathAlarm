package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.CHECKBOX_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.TEXT_FONT_SIZE

@Composable
fun TextWithCheckbox(
    modifier: Modifier = Modifier,
    text: String,
    initialState: Boolean,
    onCheckChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = CenterVertically,
    ) {
        Checkbox(
            modifier = Modifier.padding(end = CHECKBOX_END_PADDING),
            checked = initialState,
            onCheckedChange = { onCheckChange(it) },
        )
        Text(
            text = text,
            fontSize = TEXT_FONT_SIZE,
            fontWeight = Normal,
        )
    }
}

@Preview
@Composable
private fun TextWithCheckboxPreview() {
    MaterialTheme {
        TextWithCheckbox(text = "Notify", initialState = true) {}
    }
}

private object TextWithCheckbox {
    val CHECKBOX_END_PADDING = 14.dp
    val TEXT_FONT_SIZE = 16.sp
}
