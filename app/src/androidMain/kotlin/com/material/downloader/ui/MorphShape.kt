package com.material.downloader.ui

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

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

@Composable
fun ExpressiveShapeLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
) {
    val polygons = remember {
        listOf(
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.72f,
                rounding = CornerRounding(0.45f)
            ).normalized(),
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.68f,
                rounding = CornerRounding(0.55f)
            ).normalized(),
            RoundedPolygon(
                numVertices = 5,
                rounding = CornerRounding(0.35f)
            ).normalized(),
            RoundedPolygon(
                numVertices = 8,
                rounding = CornerRounding(0.85f)
            ).normalized()
        )
    }

    val morphs = remember(polygons) {
        polygons.indices.map { i ->
            Morph(polygons[i], polygons[(i + 1) % polygons.size])
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "expressive_loader")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = polygons.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morph_progress"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loader_rotation"
    )

    val currentIndex = progress.toInt() % morphs.size
    val rawFraction = progress - progress.toInt()
    val easedFraction = FastOutSlowInEasing.transform(rawFraction)

    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = containerColor,
        modifier = modifier.size(size + 24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { rotationZ = rotation }
            ) {
                val androidPath = morphs[currentIndex].toPath(progress = easedFraction)
                val matrix = android.graphics.Matrix()
                // Normalized polygons span [0, 1] or [-1, 1]; compute bounds to fit cleanly
                val bounds = RectF()
                androidPath.computeBounds(bounds, true)
                val maxDim = maxOf(bounds.width(), bounds.height()).coerceAtLeast(0.001f)
                val scale = (minOf(this.size.width, this.size.height) * 0.78f) / maxDim
                matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
                matrix.postScale(scale, scale)
                matrix.postTranslate(this.size.width / 2f, this.size.height / 2f)
                androidPath.transform(matrix)
                drawPath(path = androidPath.asComposePath(), color = color)
            }
        }
    }
}
