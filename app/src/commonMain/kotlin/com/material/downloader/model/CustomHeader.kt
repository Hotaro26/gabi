package com.material.downloader.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomHeader(
    val id: String,
    val key: String,
    val value: String,
    val isEnabled: Boolean = true
)
