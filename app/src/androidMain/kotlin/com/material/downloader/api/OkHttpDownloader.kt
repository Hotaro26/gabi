package com.material.downloader.api

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())
        
        for ((key, values) in request.headers()) {
            for (value in values) {
                builder.addHeader(key, value)
            }
        }

        val httpMethod = request.httpMethod()
        val body = if (httpMethod == "POST" || httpMethod == "PUT") {
            request.dataToSend()?.toRequestBody() ?: "".toRequestBody()
        } else null

        builder.method(httpMethod, body)

        client.newCall(builder.build()).execute().use { response ->
            val headers = mutableMapOf<String, List<String>>()
            response.headers.names().forEach { name ->
                headers[name] = response.headers.values(name)
            }
            
            return Response(
                response.code,
                response.message,
                headers,
                response.body?.string() ?: "",
                response.request.url.toString()
            )
        }
    }
}
