package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.IconEndPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TextFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TextWithIconHorizontalPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithIcon.TextWithIconTopPadding

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
                top = TextWithIconTopPadding,
                start = TextWithIconHorizontalPadding,
                end = TextWithIconHorizontalPadding,
            )
            .fillMaxWidth(),
    ) {
        Icon(
            modifier = Modifier.padding(end = IconEndPadding),
            imageVector = image,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.clickable { onClick?.invoke() },
            text = text,
            fontSize = TextFontSize,
            fontWeight = Normal,
        )
    }
}

private object TextWithIcon {
    val TextFontSize = 16.sp
    val IconEndPadding = 14.dp
    val TextWithIconTopPadding = 30.dp
    val TextWithIconHorizontalPadding = 10.dp
}
