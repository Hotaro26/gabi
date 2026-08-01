package com.material.downloader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val YoutubeOutline: ImageVector
    get() {
        if (_youtubeOutline != null) {
            return _youtubeOutline!!
        }
        _youtubeOutline = ImageVector.Builder(
            name = "YoutubeOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.58f, 7.19f)
                curveTo(21.36f, 6.36f, 20.73f, 5.71f, 19.9f, 5.49f)
                curveTo(18.42f, 5.1f, 12f, 5.1f, 12f, 5.1f)
                curveTo(12f, 5.1f, 5.58f, 5.1f, 4.1f, 5.49f)
                curveTo(3.27f, 5.71f, 2.64f, 6.36f, 2.42f, 7.19f)
                curveTo(2.02f, 8.68f, 2.02f, 12f, 2.02f, 12f)
                curveTo(2.02f, 12f, 2.02f, 15.32f, 2.42f, 16.81f)
                curveTo(2.64f, 17.64f, 3.27f, 18.29f, 4.1f, 18.51f)
                curveTo(5.58f, 18.9f, 12f, 18.9f, 12f, 18.9f)
                curveTo(12f, 18.9f, 18.42f, 18.9f, 19.9f, 18.51f)
                curveTo(20.73f, 18.29f, 21.36f, 17.64f, 21.58f, 16.81f)
                curveTo(21.98f, 15.32f, 21.98f, 12f, 21.98f, 12f)
                curveTo(21.98f, 12f, 21.98f, 8.68f, 21.58f, 7.19f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.75f, 15.02f)
                lineTo(15.5f, 12f)
                lineTo(9.75f, 8.98f)
                close()
            }
        }.build()
        return _youtubeOutline!!
    }

private var _youtubeOutline: ImageVector? = null
