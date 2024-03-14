package com.timilehinaregbesola.mathalarm.presentation.ui

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection



val fabShape: Shape = object: Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val baseWidth = 84f
        val baseHeight = 88f

        val path = Path().apply {
            moveTo(40.538f, 0f)
            cubicTo(29.3416f, 0f, 21.1903f, 5.5391f, 13.8527f, 12.8765f)
            cubicTo(6.5156f, 20.2136f, 0f, 29.3417f, 0f, 40.5382f)
            cubicTo(0f, 51.7346f, 4.5363f, 61.8655f, 11.8736f, 69.2029f)
            cubicTo(19.2107f, 76.54f, 29.3416f, 81.0764f, 40.538f, 81.0764f)
            cubicTo(51.7347f, 81.0764f, 61.8655f, 76.54f, 69.2027f, 69.2029f)
            cubicTo(76.54f, 61.8655f, 77.7019f, 55.9083f, 77.7019f, 44.7119f)
            cubicTo(77.7019f, 22.3228f, 62.9274f, 0f, 40.538f, 0f)
            close()
        }

        return Outline.Generic(
            path
                .asAndroidPath()
                .apply {
                    transform(Matrix().apply {
                        setScale(size.width / baseWidth, size.height / baseHeight)
                    })
                }
                .asComposePath()
        )
    }
}
