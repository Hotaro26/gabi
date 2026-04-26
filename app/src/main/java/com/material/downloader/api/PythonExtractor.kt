package com.material.downloader.api

import com.chaquo.python.Python
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PythonExtractor {
    private val py = Python.getInstance()
    private val module = py.getModule("downloader")
    private val json = Json { ignoreUnknownKeys = true }

    fun extract(url: String, quality: String, mode: String, engine: String): ExtractionResult {
        return try {
            val resultJson = module.callAttr("extract_info", url, quality, mode, engine).toString()
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
    val title: String? = null,
    val author: String? = null,
    val thumbnail: String? = null,
    val size: Long? = null,
    val ext: String? = null,
    val message: String? = null
)
