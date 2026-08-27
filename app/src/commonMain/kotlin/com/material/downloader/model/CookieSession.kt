package com.material.downloader.model

import kotlinx.serialization.Serializable

@Serializable
data class CookieSession(
    val id: String,
    val domain: String,
    val cookieString: String,
    val engine: String
)
