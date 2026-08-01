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
        customFolderUri: String? = null,
        onUriCreated: ((Uri) -> Unit)? = null
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        
        try {
            client.prepareGet(url) {
                header(io.ktor.http.HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                header("Accept", "*/*")
            }.execute { response ->
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
                
                onUriCreated?.invoke(uri)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    var bytesRead = 0L
                    val buffer = ByteArray(8192)
                    var lastEmitTime = System.currentTimeMillis()
                    var bytesSinceLastEmit = 0L
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                            bytesRead += read
                            bytesSinceLastEmit += read
                            val now = System.currentTimeMillis()
                            val timeDiff = now - lastEmitTime
                            if (timeDiff >= 500 || channel.isClosedForRead) {
                                val speedBps = (bytesSinceLastEmit * 1000L) / timeDiff.coerceAtLeast(1L)
                                val prog = if (contentLength > 0) bytesRead.toFloat() / contentLength else 0f
                                emit(DownloadState.Downloading(prog, bytesRead, contentLength, speedBps))
                                lastEmitTime = now
                                bytesSinceLastEmit = 0L
                            }
                        }
                    }
                }
                emit(DownloadState.Success(uri.toString()))
            }
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error"))
        }
    }

    suspend fun downloadFileToPath(
        url: String, 
        destFile: java.io.File
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        try {
            client.prepareGet(url) {
                header(io.ktor.http.HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                header("Accept", "*/*")
            }.execute { response ->
                if (response.status.value !in 200..299) {
                    throw Exception("Server returned status ${response.status.value}: ${response.status.description}")
                }

                val contentLength = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLong() ?: -1L
                val channel: ByteReadChannel = response.bodyAsChannel()
                
                destFile.outputStream().use { outputStream ->
                    var bytesRead = 0L
                    val buffer = ByteArray(8192)
                    var lastEmitTime = System.currentTimeMillis()
                    var bytesSinceLastEmit = 0L
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                            bytesRead += read
                            bytesSinceLastEmit += read
                            val now = System.currentTimeMillis()
                            val timeDiff = now - lastEmitTime
                            if (timeDiff >= 500 || channel.isClosedForRead) {
                                val speedBps = (bytesSinceLastEmit * 1000L) / timeDiff.coerceAtLeast(1L)
                                val prog = if (contentLength > 0) bytesRead.toFloat() / contentLength else 0f
                                emit(DownloadState.Downloading(prog, bytesRead, contentLength, speedBps))
                                lastEmitTime = now
                                bytesSinceLastEmit = 0L
                            }
                        }
                    }
                }
                emit(DownloadState.Success(destFile.absolutePath))
            }
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error"))
        }
    }

    fun getMimeTypeFromExtension(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            else -> "*/*"
        }
    }

    fun createMediaStoreUri(fileName: String, relativePath: String): Uri? {
        val mimeType = getMimeTypeFromExtension(fileName)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
            when {
                relativePath.startsWith("Download") -> MediaStore.Downloads.getContentUri(volume)
                mimeType.startsWith("image/") -> MediaStore.Images.Media.getContentUri(volume)
                mimeType.startsWith("audio/") -> MediaStore.Audio.Media.getContentUri(volume)
                else -> MediaStore.Video.Media.getContentUri(volume)
            }
        } else {
            when {
                mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        }

        return context.contentResolver.insert(collection, contentValues)
    }

    fun createSafUri(folderUri: Uri, fileName: String): Uri? {
        val mimeType = getMimeTypeFromExtension(fileName)
        val directory = DocumentFile.fromTreeUri(context, folderUri)
        val file = directory?.createFile(mimeType, fileName)
        return file?.uri
    }

    suspend fun finalizeFile(uri: Uri, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mimeType = getMimeTypeFromExtension(fileName)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
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
    data class Downloading(val progress: Float, val downloadedBytes: Long = 0L, val totalBytes: Long = 0L, val speedBps: Long = 0L) : DownloadState()
    data class Success(val path: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
