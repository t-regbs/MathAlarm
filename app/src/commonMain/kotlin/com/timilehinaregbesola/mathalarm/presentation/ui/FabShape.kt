package com.timilehinaregbesola.mathalarm.presentation.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection


val fabShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // The original dimensions of the shape's path definition.
        val baseWidth = 84f
        val baseHeight = 88f

        // Calculate the scaling factors.
        val scaleX = size.width / baseWidth
        val scaleY = size.height / baseHeight

        // Build the path with scaled coordinates.
        val path = Path().apply {
            // By multiplying each coordinate by the scale factor, we make the shape
            // adapt to the size of the component, just like the original Matrix.transform did.
            moveTo(40.538f * scaleX, 0f)
            cubicTo(29.3416f * scaleX, 0f, 21.1903f * scaleX, 5.5391f * scaleY, 13.8527f * scaleX, 12.8765f * scaleY)
            cubicTo(6.5156f * scaleX, 20.2136f * scaleY, 0f, 29.3417f * scaleY, 0f, 40.5382f * scaleY)
            cubicTo(0f, 51.7346f * scaleY, 4.5363f * scaleX, 61.8655f * scaleY, 11.8736f * scaleX, 69.2029f * scaleY)
            cubicTo(19.2107f * scaleX, 76.54f * scaleY, 29.3416f * scaleX, 81.0764f * scaleY, 40.538f * scaleX, 81.0764f * scaleY)
            cubicTo(51.7347f * scaleX, 81.0764f * scaleY, 61.8655f * scaleX, 76.54f * scaleY, 69.2027f * scaleX, 69.2029f * scaleY)
            cubicTo(76.54f * scaleX, 61.8655f * scaleY, 77.7019f * scaleX, 55.9083f * scaleY, 77.7019f * scaleX, 44.7119f * scaleY)
            cubicTo(77.7019f * scaleX, 22.3228f * scaleY, 62.9274f * scaleX, 0f, 40.538f * scaleX, 0f)
            close()
        }

        return Outline.Generic(path)
    }
}

