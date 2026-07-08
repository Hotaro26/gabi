package com.material.downloader.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.awt.Desktop

class DesktopPlatformBridge : PlatformBridge {
    override fun showToast(message: String) {
        println("TOAST: $message")
    }

    override suspend fun extractMediaInfo(url: String): String {
        return "{}" // Stub
    }

    override suspend fun downloadMedia(
        url: String, quality: String, isAudioOnly: Boolean,
        onProgress: (Float) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit
    ) {
        // Stub
    }

    override fun openSavedFolder(path: String) {
        try {
            val dir = File(System.getProperty("user.home"), "Downloads")
            Desktop.getDesktop().open(dir)
        } catch(e: Exception) { e.printStackTrace() }
    }

    // A simple mock for settings on desktop for now
    private val settings = mutableMapOf<String, String>()
    override fun saveSetting(key: String, value: String) {
        settings[key] = value
    }

    override fun getSetting(key: String, defaultValue: String): String {
        return settings[key] ?: defaultValue
    }
}
