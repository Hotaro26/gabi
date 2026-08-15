package com.material.downloader.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import android.graphics.RectF

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = polygon.toPath()
        val bounds = RectF()
        path.computeBounds(bounds, true)
        
        val scaleX = size.width / bounds.width()
        val scaleY = size.height / bounds.height()
        val scale = minOf(scaleX.toDouble(), scaleY.toDouble()).toFloat()
        
        val scaledPath = android.graphics.Path()
        val matrix = android.graphics.Matrix()
        
        // Scale and center
        matrix.postScale(scale, scale)
        matrix.postTranslate(
            (size.width - bounds.width() * scale) / 2f - bounds.left * scale,
            (size.height - bounds.height() * scale) / 2f - bounds.top * scale
        )
        
        path.transform(matrix, scaledPath)
        
        return Outline.Generic(scaledPath.asComposePath())
    }
}
