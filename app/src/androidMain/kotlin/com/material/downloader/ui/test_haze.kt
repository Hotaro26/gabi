package com.material.downloader.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle

@Composable
fun TestHaze() {
    val hazeState = remember { HazeState() }
    Box(modifier = Modifier.haze(state = hazeState)) {
        Box(modifier = Modifier.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = Color.Black.copy(alpha = 0.5f), blurRadius = 16.dp)))
    }
}
