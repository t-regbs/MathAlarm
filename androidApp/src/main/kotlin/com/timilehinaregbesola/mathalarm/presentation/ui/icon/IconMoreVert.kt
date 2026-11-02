package com.timilehinaregbesola.mathalarm.presentation.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MoreVert: ImageVector by lazy {
    ImageVector.Builder("MoreVert", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            for (y in listOf(5f, 12f, 19f)) {
                moveTo(14f, y)
                arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
                close()
            }
        }
    }.build()
}
