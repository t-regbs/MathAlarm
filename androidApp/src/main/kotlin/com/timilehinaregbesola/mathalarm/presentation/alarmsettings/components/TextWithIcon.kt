package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TEXT_WITH_ICON_TOP_PADDING
import androidx.compose.ui.tooling.preview.Preview
import com.mohamedrejeb.calf.ui.gesture.adaptiveClickable
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Notifications

@Composable
fun TextWithIcon(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .padding(top = TEXT_WITH_ICON_TOP_PADDING)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        SettingsLeadingIcon(image)
        Text(
            modifier = Modifier.adaptiveClickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
            ),
            text = text,
            fontSize = TEXT_FONT_SIZE,
            fontWeight = Normal,
        )
    }
}

@Preview
@Composable
private fun TextWithIconPreview() {
    MaterialTheme {
        TextWithIcon(image = Notifications, text = "Notify")
    }
}

private object TextWithIcon {
    val TEXT_FONT_SIZE = 16.sp
    val TEXT_WITH_ICON_TOP_PADDING = 30.dp
}
