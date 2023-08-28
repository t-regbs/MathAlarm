package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.ICON_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TEXT_WITH_ICON_HORIZONTAL_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TEXT_WITH_ICON_TOP_PADDING

@Composable
fun TextWithIcon(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .padding(
                top = TEXT_WITH_ICON_TOP_PADDING,
                start = TEXT_WITH_ICON_HORIZONTAL_PADDING,
                end = TEXT_WITH_ICON_HORIZONTAL_PADDING,
            )
            .fillMaxWidth(),
    ) {
        Icon(
            modifier = Modifier.padding(end = ICON_END_PADDING),
            imageVector = image,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.clickable { onClick?.invoke() },
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
        TextWithIcon(image = Icons.Outlined.Notifications, text = "Notify")
    }
}

private object TextWithIcon {
    val TEXT_FONT_SIZE = 16.sp
    val ICON_END_PADDING = 14.dp
    val TEXT_WITH_ICON_TOP_PADDING = 30.dp
    val TEXT_WITH_ICON_HORIZONTAL_PADDING = 10.dp
}
