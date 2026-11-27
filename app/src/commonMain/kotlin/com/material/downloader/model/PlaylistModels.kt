package com.material.downloader.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItem(
    val id: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null,
    val isSelected: Boolean = true
)

@Serializable
data class PlaylistInfo(
    val id: String,
    val title: String,
    val author: String? = null,
    val thumbnail: String? = null,
    val itemCount: Int,
    val items: List<PlaylistItem> = emptyList()
)
