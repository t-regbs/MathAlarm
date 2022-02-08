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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextWithIcon(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .padding(top = 30.dp, start = 10.dp, end = 10.dp)
            .fillMaxWidth()
    ) {
        Icon(
            modifier = Modifier.padding(end = 14.dp),
            imageVector = image,
            contentDescription = null
        )
        Text(
            modifier = Modifier.clickable { onClick?.invoke() },
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
