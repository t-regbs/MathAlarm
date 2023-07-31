package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.CheckboxEndPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.TextFontSize

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
            modifier = Modifier.padding(end = CheckboxEndPadding),
            checked = initialState,
            onCheckedChange = { onCheckChange(it) },
        )
        Text(
            text = text,
            fontSize = TextFontSize,
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
    val CheckboxEndPadding = 14.dp
    val TextFontSize = 16.sp
}
