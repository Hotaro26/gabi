package com.material.downloader.ui

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSwipe() {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(state = state, backgroundContent = {}, content = {})
}
