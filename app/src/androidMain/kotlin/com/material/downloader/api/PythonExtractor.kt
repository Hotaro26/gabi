package com.material.downloader.api

import com.chaquo.python.Python
import com.material.downloader.model.PlaylistItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PythonExtractor {
    private val py = Python.getInstance()
    private val module = py.getModule("downloader")
    private val json = Json { ignoreUnknownKeys = true }

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
            val resultJson = module.callAttr("extract_info", url, quality, mode, engine, cookiesPath, userAgent, customHeadersJson).toString()
            json.decodeFromString<ExtractionResult>(resultJson)
        } catch (e: Exception) {
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
            val resultJson = module.callAttr("extract_playlist", url, cookiesPath, userAgent, customHeadersJson).toString()
            json.decodeFromString<ExtractionResult>(resultJson)
        } catch (e: Exception) {
            ExtractionResult(status = "error", message = e.message ?: "Playlist extraction failed")
        }
    }

    fun downloadVideo(url: String, outputPath: String, cookiesPath: String? = null, customHeadersJson: String? = null): Map<String, String> {
        return try {
            val resultJson = module.callAttr("download_video", url, outputPath, cookiesPath, customHeadersJson).toString()
            json.decodeFromString<Map<String, String>>(resultJson)
        } catch (e: Exception) {
            mapOf("status" to "error", "message" to (e.message ?: "Python execution failed"))
        }
    }

    fun getVersions(): Map<String, String> {
        return try {
            val resultJson = module.callAttr("get_versions").toString()
            val map = json.decodeFromString<Map<String, String>>(resultJson)
            map
        } catch (e: Exception) {
            mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
        }
    }

    fun updateExtractors(targetPath: String): Map<String, String> {
        return try {
            val resultJson = module.callAttr("update_extractors", targetPath).toString()
            json.decodeFromString<Map<String, String>>(resultJson)
        } catch (e: Exception) {
            mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
        }
    }
}

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
    val playlist_items: List<PlaylistItem>? = null
)
