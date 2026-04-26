package com.material.downloader.model

import kotlinx.serialization.Serializable

@Serializable
data class CobaltRequest(
    val url: String,
    val videoQuality: String = "720",
    val audioFormat: String = "mp3",
    val downloadMode: String = "auto"
)

@Serializable
data class CobaltResponse(
    val status: String,
    val url: String? = null,
    val text: String? = null,
    val picker: List<PickerItem>? = null
)

@Serializable
data class PickerItem(
    val type: String? = null,
    val url: String,
    val thumb: String? = null,
    val quality: String? = null
)
