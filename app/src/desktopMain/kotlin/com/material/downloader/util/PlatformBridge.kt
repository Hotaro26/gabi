package com.material.downloader.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader

class DesktopPlatformBridge : PlatformBridge {
    override fun showToast(message: String) {
        // Fallback for Desktop: simply print to standard output
        println("TOAST: $message")
    }

    override suspend fun extractMediaInfo(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Assuming yt-dlp is available in system PATH
                val process = ProcessBuilder("yt-dlp", "--dump-json", url)
                    .redirectErrorStream(true)
                    .start()
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                process.waitFor()
                output.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                "{}" // Return empty JSON on failure
            }
        }
    }

    override suspend fun downloadMedia(
        url: String,
        quality: String,
        isAudioOnly: Boolean,
        onProgress: (Float) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val downloadsDir = File(System.getProperty("user.home"), "Downloads")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val formatArgs = if (isAudioOnly) {
                    listOf("-x", "--audio-format", "mp3", "-o", "${downloadsDir.absolutePath}/%(title)s.%(ext)s")
                } else {
                    listOf("-f", "bestvideo[height<=$quality]+bestaudio/best", "-o", "${downloadsDir.absolutePath}/%(title)s.%(ext)s")
                }

                val command = mutableListOf("yt-dlp", url)
                command.addAll(formatArgs)

                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()

                // A simple reader to keep process alive and capture output, no advanced progress parsing yet
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // For a robust implementation, parse yt-dlp output to calculate progress
                    onProgress(0.5f) // Fake progress for now
                }
                process.waitFor()

                if (process.exitValue() == 0) {
                    onComplete("Downloaded to ${downloadsDir.absolutePath}")
                } else {
                    onError("Download failed with exit code ${process.exitValue()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
