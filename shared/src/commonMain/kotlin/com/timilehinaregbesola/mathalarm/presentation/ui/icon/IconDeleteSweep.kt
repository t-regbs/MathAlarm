package com.timilehinaregbesola.mathalarm.presentation.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val DeleteSweep: ImageVector
    get() {
        if (_DeleteSweep != null) {
            return _DeleteSweep!!
        }
        _DeleteSweep = ImageVector.Builder(
            name = "DeleteSweep",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(600f, 720f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(80f)
                lineTo(600f, 720f)
                close()
                moveTo(600f, 400f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(280f)
                verticalLineToRelative(80f)
                lineTo(600f, 400f)
                close()
                moveTo(600f, 560f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(80f)
                lineTo(600f, 560f)
                close()
                moveTo(120f, 320f)
                lineTo(80f, 320f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-60f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(60f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(-40f)
                verticalLineToRelative(360f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(440f, 760f)
                lineTo(200f, 760f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 680f)
                verticalLineToRelative(-360f)
                close()
                moveTo(200f, 320f)
                verticalLineToRelative(360f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(-360f)
                lineTo(200f, 320f)
                close()
                moveTo(200f, 320f)
                verticalLineToRelative(360f)
                verticalLineToRelative(-360f)
                close()
            }
        }.build()

        return _DeleteSweep!!
    }

@Suppress("ObjectPropertyName")
private var _DeleteSweep: ImageVector? = null
