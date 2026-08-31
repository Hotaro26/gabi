package com.material.downloader.api

import com.material.downloader.model.CobaltRequest
import com.material.downloader.model.CobaltResponse
import io.ktor.client.*
import io.ktor.client.call.*

import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CobaltRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private val client = HttpClient(io.ktor.client.engine.android.Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) { Log.d("CobaltAPI", message) }
            }
            level = LogLevel.ALL
        }
    }

    suspend fun fetchMediaLink(
        url: String,
        quality: String,
        audioFormat: String = "mp3",
        downloadMode: String = "auto"
    ): CobaltResponse {
        val requestBody = CobaltRequest(
            url = url,
            videoQuality = quality,
            audioFormat = audioFormat,
            downloadMode = downloadMode
        )
        val response = client.post("https://hotaro344yy-cobalt-api.hf.space") {
            headers {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
                set("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            }
            setBody(json.encodeToString(requestBody))
        }
        
        return json.decodeFromString(response.bodyAsText())
    }
    
    fun getClient() = HttpClient(io.ktor.client.engine.android.Android) {
        install(HttpTimeout) { requestTimeoutMillis = 600_000 }
    }
}
