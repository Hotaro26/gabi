package com.material.downloader.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.OutputStream

class FileDownloader(private val context: Context, private val client: HttpClient) {

    suspend fun downloadFile(
        url: String, 
        fileName: String, 
        relativePath: String = "Movies/ExpressiveDownloader",
        customFolderUri: String? = null
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        
        try {
            val response = client.get(url)
            
            if (response.status.value !in 200..299) {
                throw Exception("Server returned status ${response.status.value}: ${response.status.description}")
            }

            val contentLength = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLong() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()
            
            val uri = if (customFolderUri != null) {
                createSafUri(Uri.parse(customFolderUri), fileName)
            } else {
                createMediaStoreUri(fileName, relativePath)
            } ?: throw Exception("Could not create destination file")
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                var bytesRead = 0L
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(buffer.size.toLong())
                    while (!packet.isEmpty) {
                        val length = packet.remaining.toInt()
                        packet.readAvailable(buffer, 0, length)
                        outputStream.write(buffer, 0, length)
                        bytesRead += length
                        
                        if (contentLength > 0) {
                            emit(DownloadState.Downloading(bytesRead.toFloat() / contentLength))
                        }
                    }
                }
            }
            
            emit(DownloadState.Success(uri.toString()))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error"))
        }
    }

    private fun createMediaStoreUri(fileName: String, relativePath: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (relativePath.startsWith("Download", ignoreCase = true)) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        return context.contentResolver.insert(collection, contentValues)
    }

    private fun createSafUri(folderUri: Uri, fileName: String): Uri? {
        val directory = DocumentFile.fromTreeUri(context, folderUri)
        val file = directory?.createFile("video/mp4", fileName)
        return file?.uri
    }

    suspend fun finalizeVideo(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            try {
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                // Ignore if not a MediaStore URI
            }
        }
    }
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Success(val path: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
