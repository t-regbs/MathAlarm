package com.timilehinaregbesola.mathalarm.presentation.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val WbSunny: ImageVector
    get() {
        if (_WbSunny != null) {
            return _WbSunny!!
        }
        _WbSunny = ImageVector.Builder(
            name = "WbSunny",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(440f, 160f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(440f, 920f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(800f, 520f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(80f)
                lineTo(800f, 520f)
                close()
                moveTo(40f, 520f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(80f)
                lineTo(40f, 520f)
                close()
                moveTo(748f, 268f)
                lineTo(692f, 212f)
                lineTo(762f, 140f)
                lineTo(820f, 198f)
                lineTo(748f, 268f)
                close()
                moveTo(198f, 820f)
                lineToRelative(-58f, -58f)
                lineToRelative(72f, -70f)
                lineToRelative(56f, 56f)
                lineToRelative(-70f, 72f)
                close()
                moveTo(762f, 820f)
                lineTo(692f, 748f)
                lineTo(748f, 692f)
                lineTo(820f, 762f)
                lineTo(762f, 820f)
                close()
                moveTo(212f, 268f)
                lineToRelative(-72f, -70f)
                lineToRelative(58f, -58f)
                lineToRelative(70f, 72f)
                lineToRelative(-56f, 56f)
                close()
                moveTo(480f, 720f)
                quadToRelative(-100f, 0f, -170f, -70f)
                reflectiveQuadToRelative(-70f, -170f)
                quadToRelative(0f, -100f, 70f, -170f)
                reflectiveQuadToRelative(170f, -70f)
                quadToRelative(100f, 0f, 170f, 70f)
                reflectiveQuadToRelative(70f, 170f)
                quadToRelative(0f, 100f, -70f, 170f)
                reflectiveQuadToRelative(-170f, 70f)
                close()
            }
        }.build()

        return _WbSunny!!
    }

@Suppress("ObjectPropertyName")
private var _WbSunny: ImageVector? = null
