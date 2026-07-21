package com.material.downloader.api

import com.chaquo.python.Python
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PythonExtractor {
    private val py = Python.getInstance()
    private val module = py.getModule("downloader")
    private val json = Json { ignoreUnknownKeys = true }

    fun extract(url: String, quality: String, mode: String, engine: String, cookiesPath: String? = null): ExtractionResult {
        return try {
            val resultJson = module.callAttr("extract_info", url, quality, mode, engine, cookiesPath).toString()
            json.decodeFromString<ExtractionResult>(resultJson)
        } catch (e: Exception) {
            ExtractionResult(status = "error", message = e.message ?: "Python execution failed")
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
    val available_qualities: List<String>? = null
)
