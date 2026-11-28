package com.material.downloader.api

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.material.downloader.model.PlaylistItem
import com.material.downloader.util.GabiLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PythonExtractor {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        @Volatile
        var appContext: Context? = null

        private val initLock = Any()

        fun ensureStarted(context: Context? = appContext) {
            if (Python.isStarted()) return
            synchronized(initLock) {
                if (!Python.isStarted()) {
                    val ctx = context ?: appContext
                    if (ctx != null) {
                        Python.start(AndroidPlatform(ctx.applicationContext))
                        val customPath = java.io.File(ctx.filesDir, "python_packages").absolutePath
                        Python.getInstance().getModule("sys").get("path")?.callAttr("insert", 0, customPath)
                    } else {
                        // Wait briefly if MainActivity IO coroutine is mid-start
                        var waited = 0
                        while (!Python.isStarted() && waited < 5000) {
                            Thread.sleep(50)
                            waited += 50
                        }
                    }
                }
            }
        }
    }

    private fun getModule(): PyObject {
        ensureStarted()
        return Python.getInstance().getModule("downloader")
    }

    fun extract(
        url: String, 
        quality: String, 
        mode: String, 
        engine: String, 
        cookiesPath: String? = null, 
        userAgent: String? = null,
        customHeadersJson: String? = null
    ): ExtractionResult {
        return try {
            GabiLogger.d("PythonExtractor", "Starting extraction: url=$url, quality=$quality, mode=$mode, engine=$engine")
            val resultJson = getModule().callAttr("extract_info", url, quality, mode, engine, cookiesPath, userAgent, customHeadersJson).toString()
            json.decodeFromString<ExtractionResult>(resultJson)
        } catch (e: Exception) {
            GabiLogger.e("PythonExtractor", "Extraction failed for $url: ${e.message}", e)
            ExtractionResult(status = "error", message = e.message ?: "Python execution failed")
        }
    }

    fun extractPlaylist(
        url: String,
        cookiesPath: String? = null,
        userAgent: String? = null,
        customHeadersJson: String? = null
    ): ExtractionResult {
        return try {
            GabiLogger.d("PythonExtractor", "Starting playlist extraction: url=$url")
            val resultJson = getModule().callAttr("extract_playlist", url, cookiesPath, userAgent, customHeadersJson).toString()
            json.decodeFromString<ExtractionResult>(resultJson)
        } catch (e: Exception) {
            GabiLogger.e("PythonExtractor", "Playlist extraction failed for $url: ${e.message}", e)
            ExtractionResult(status = "error", message = e.message ?: "Playlist extraction failed")
        }
    }

    fun downloadVideo(url: String, outputPath: String, cookiesPath: String? = null, customHeadersJson: String? = null): Map<String, String> {
        return try {
            GabiLogger.d("PythonExtractor", "Starting downloadVideo: url=$url -> $outputPath")
            val resultJson = getModule().callAttr("download_video", url, outputPath, cookiesPath, customHeadersJson).toString()
            json.decodeFromString<Map<String, String>>(resultJson)
        } catch (e: Exception) {
            GabiLogger.e("PythonExtractor", "downloadVideo failed: ${e.message}", e)
            mapOf("status" to "error", "message" to (e.message ?: "Python execution failed"))
        }
    }

    interface YtdlpProgressCallback {
        fun onProgress(progress: Float, speedBps: Long, downloadedBytes: Long, totalBytes: Long)
        fun isCancelled(): Boolean
    }

    fun downloadMediaYtdlp(
        url: String,
        outputDir: String,
        quality: String = "720",
        mode: String = "auto",
        cookiesPath: String? = null,
        userAgent: String? = null,
        customHeadersJson: String? = null,
        callback: YtdlpProgressCallback? = null
    ): YtdlpDownloadResult {
        return try {
            GabiLogger.d("PythonExtractor", "Starting ytdlnis-style download: url=$url, quality=$quality, mode=$mode")
            val resultJson = getModule().callAttr(
                "download_media_ytdlp",
                url,
                outputDir,
                quality,
                mode,
                cookiesPath,
                userAgent,
                customHeadersJson,
                callback
            ).toString()
            json.decodeFromString<YtdlpDownloadResult>(resultJson)
        } catch (e: Exception) {
            GabiLogger.e("PythonExtractor", "downloadMediaYtdlp failed: ${e.message}", e)
            YtdlpDownloadResult(status = "error", message = e.message ?: "yt-dlp execution failed")
        }
    }

    fun getVersions(): Map<String, String> {
        return try {
            val resultJson = getModule().callAttr("get_versions").toString()
            val map = json.decodeFromString<Map<String, String>>(resultJson)
            map
        } catch (e: Exception) {
            mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
        }
    }

    fun updateExtractors(targetPath: String): Map<String, String> {
        return try {
            val resultJson = getModule().callAttr("update_extractors", targetPath).toString()
            json.decodeFromString<Map<String, String>>(resultJson)
        } catch (e: Exception) {
            mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
        }
    }
}

@Serializable
data class YtdlpDownloadResult(
    val status: String,
    val file_path: String? = null,
    val video_path: String? = null,
    val audio_path: String? = null,
    val title: String? = null,
    val author: String? = null,
    val thumbnail: String? = null,
    val ext: String? = null,
    val message: String? = null,
    val extractor: String? = null,
    val media_id: String? = null
)

@Serializable
data class ExtractionResult(
    val status: String,
    val url: String? = null,
    val urls: List<String>? = null,
    val title: String? = null,
    val author: String? = null,
    val thumbnail: String? = null,
    val size: Long? = null,
    val ext: String? = null,
    val is_gallery: Boolean? = null,
    val message: String? = null,
    val audio_url: String? = null,
    val max_resolution: String? = null,
    val available_qualities: List<String>? = null,
    val http_headers: Map<String, String>? = null,
    val is_playlist: Boolean? = null,
    val playlist_id: String? = null,
    val playlist_title: String? = null,
    val playlist_items: List<PlaylistItem>? = null,
    val extractor: String? = null,
    val media_id: String? = null
)
