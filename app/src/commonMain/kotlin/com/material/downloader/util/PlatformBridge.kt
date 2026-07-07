package com.material.downloader.util

interface PlatformBridge {
    fun showToast(message: String)
    suspend fun extractMediaInfo(url: String): String
    suspend fun downloadMedia(
        url: String, 
        quality: String, 
        isAudioOnly: Boolean,
        onProgress: (Float) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    )
    fun openSavedFolder(path: String)
    fun saveSetting(key: String, value: String)
    fun getSetting(key: String, defaultValue: String): String
}
