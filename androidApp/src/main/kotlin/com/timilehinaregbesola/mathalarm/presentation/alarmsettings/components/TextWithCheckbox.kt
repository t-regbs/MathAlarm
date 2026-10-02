package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.DISABLED_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.ENABLED_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox.TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TextWithCheckbox(
    modifier: Modifier = Modifier,
    text: String,
    initialState: Boolean,
    enabled: Boolean = true,
    onCheckChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier.alpha(if (enabled) ENABLED_ALPHA else DISABLED_ALPHA)
            .toggleable(value = initialState, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckChange),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Checkbox(checked = initialState, enabled = enabled, onCheckedChange = null)
        }
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
    val TEXT_FONT_SIZE = 16.sp
    const val ENABLED_ALPHA = 1f
    const val DISABLED_ALPHA = 0.6f
}
