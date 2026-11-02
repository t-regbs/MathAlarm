package com.timilehinaregbesola.mathalarm.presentation.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val EmojiSymbols: ImageVector
    get() {
        if (_EmojiSymbols != null) {
            return _EmojiSymbols!!
        }
        _EmojiSymbols = ImageVector.Builder(
            name = "EmojiSymbols",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(120f, 160f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(80f)
                lineTo(120f, 160f)
                close()
                moveTo(240f, 440f)
                verticalLineToRelative(-160f)
                lineTo(120f, 280f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(80f)
                lineTo(320f, 280f)
                verticalLineToRelative(160f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(548f, 864f)
                lineToRelative(-56f, -56f)
                lineToRelative(312f, -312f)
                lineToRelative(56f, 56f)
                lineTo(548f, 864f)
                close()
                moveTo(580f, 640f)
                quadToRelative(-26f, 0f, -43f, -17f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(0f, -26f, 17f, -43f)
                reflectiveQuadToRelative(43f, -17f)
                quadToRelative(26f, 0f, 43f, 17f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(0f, 26f, -17f, 43f)
                reflectiveQuadToRelative(-43f, 17f)
                close()
                moveTo(780f, 840f)
                quadToRelative(-26f, 0f, -43f, -17f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(0f, -26f, 17f, -43f)
                reflectiveQuadToRelative(43f, -17f)
                quadToRelative(26f, 0f, 43f, 17f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(0f, 26f, -17f, 43f)
                reflectiveQuadToRelative(-43f, 17f)
                close()
                moveTo(620f, 440f)
                quadToRelative(-41f, 0f, -70.5f, -29.5f)
                reflectiveQuadTo(520f, 340f)
                quadToRelative(0f, -41f, 29.5f, -71.5f)
                reflectiveQuadTo(620f, 238f)
                quadToRelative(12f, 0f, 21.5f, 1.5f)
                reflectiveQuadTo(660f, 244f)
                verticalLineToRelative(-124f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(700f, 80f)
                horizontalLineToRelative(140f)
                verticalLineToRelative(80f)
                lineTo(720f, 160f)
                verticalLineToRelative(180f)
                quadToRelative(0f, 41f, -29.5f, 70.5f)
                reflectiveQuadTo(620f, 440f)
                close()
                moveTo(220f, 880f)
                quadToRelative(-41f, 0f, -70.5f, -30.5f)
                reflectiveQuadTo(120f, 778f)
                quadToRelative(0f, -18f, 7.5f, -36.5f)
                reflectiveQuadTo(150f, 708f)
                lineToRelative(42f, -42f)
                lineToRelative(-14f, -14f)
                quadToRelative(-15f, -15f, -22.5f, -32.5f)
                reflectiveQuadTo(148f, 582f)
                quadToRelative(0f, -41f, 29.5f, -70.5f)
                reflectiveQuadTo(248f, 482f)
                quadToRelative(41f, 0f, 70.5f, 29.5f)
                reflectiveQuadTo(348f, 582f)
                quadToRelative(0f, 20f, -6.5f, 37.5f)
                reflectiveQuadTo(320f, 652f)
                lineToRelative(-14f, 14f)
                lineToRelative(28f, 28f)
                lineToRelative(56f, -56f)
                lineToRelative(56f, 58f)
                lineToRelative(-56f, 56f)
                lineToRelative(56f, 56f)
                lineToRelative(-56f, 56f)
                lineToRelative(-56f, -56f)
                lineToRelative(-42f, 42f)
                quadToRelative(-15f, 15f, -33.5f, 22.5f)
                reflectiveQuadTo(220f, 880f)
                close()
                moveTo(248f, 610f)
                lineTo(262f, 596f)
                quadToRelative(3f, -3f, 4.5f, -6f)
                reflectiveQuadToRelative(1.5f, -8f)
                quadToRelative(0f, -9f, -6f, -14.5f)
                reflectiveQuadToRelative(-14f, -5.5f)
                quadToRelative(-8f, 0f, -14f, 5.5f)
                reflectiveQuadToRelative(-6f, 14.5f)
                quadToRelative(0f, 3f, 1.5f, 7f)
                reflectiveQuadToRelative(4.5f, 7f)
                lineToRelative(14f, 14f)
                close()
                moveTo(218f, 800f)
                quadToRelative(3f, 0f, 8f, -1.5f)
                reflectiveQuadToRelative(8f, -4.5f)
                lineToRelative(44f, -42f)
                lineToRelative(-28f, -28f)
                lineToRelative(-44f, 42f)
                quadToRelative(-3f, 3f, -4.5f, 7f)
                reflectiveQuadToRelative(-1.5f, 9f)
                quadToRelative(0f, 8f, 5f, 13f)
                reflectiveQuadToRelative(13f, 5f)
                close()
            }
        }.build()

        return _EmojiSymbols!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiSymbols: ImageVector? = null
