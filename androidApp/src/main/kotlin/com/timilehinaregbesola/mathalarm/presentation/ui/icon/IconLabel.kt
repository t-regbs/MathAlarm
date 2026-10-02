package com.timilehinaregbesola.mathalarm.presentation.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Label: ImageVector
    get() {
        if (_Label != null) {
            return _Label!!
        }
        _Label = ImageVector.Builder(
            name = "Label",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-480f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 160f)
                horizontalLineToRelative(440f)
                quadToRelative(19f, 0f, 36f, 8.5f)
                reflectiveQuadToRelative(28f, 23.5f)
                lineToRelative(216f, 288f)
                lineToRelative(-216f, 288f)
                quadToRelative(-11f, 15f, -28f, 23.5f)
                reflectiveQuadToRelative(-36f, 8.5f)
                lineTo(160f, 800f)
                close()
                moveTo(160f, 720f)
                horizontalLineToRelative(440f)
                lineToRelative(180f, -240f)
                lineToRelative(-180f, -240f)
                lineTo(160f, 240f)
                verticalLineToRelative(480f)
                close()
                moveTo(380f, 480f)
                close()
            }
        }.build()

        return _Label!!
    }

@Suppress("ObjectPropertyName")
private var _Label: ImageVector? = null
