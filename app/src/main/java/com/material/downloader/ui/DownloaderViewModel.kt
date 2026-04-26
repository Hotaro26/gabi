package com.material.downloader.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.material.downloader.api.CobaltRepository
import com.material.downloader.util.DownloadState
import com.material.downloader.util.FileDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.material.downloader.api.PythonExtractor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import io.ktor.client.plugins.*
import io.ktor.client.request.*

import androidx.room.Room
import com.material.downloader.db.AppDatabase
import com.material.downloader.model.DownloadLog
import com.material.downloader.util.NotificationHelper

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.material.downloader.api.ExtractionResult
import com.material.downloader.ui.theme.AppTheme

import kotlinx.coroutines.Job

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {
    private val extractor = PythonExtractor()
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "gabi_db").build()
    private val logDao = db.downloadLogDao()
    private val notificationHelper = NotificationHelper(application)
    
    private var currentDownloadJob: Job? = null
    private var lastNotificationId: Int? = null

    private val _uiState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val uiState: StateFlow<DownloadState> = _uiState

    private val _previewMetadata = MutableStateFlow<ExtractionResult?>(null)
    val previewMetadata: StateFlow<ExtractionResult?> = _previewMetadata

    var downloadPath = mutableStateOf("Movies/Gabi")
    var selectedFolderUri = mutableStateOf<String?>(null)
    var selectedFolderName = mutableStateOf<String?>(null)

    // Appearance Settings
    var themeMode = mutableIntStateOf(0)
    var selectedTheme = mutableStateOf(AppTheme.Dynamic)
    
    // External Share Support
    private val _externalUrl = MutableStateFlow<String?>(null)
    val externalUrl: StateFlow<String?> = _externalUrl

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        install(DefaultRequest) {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        }
    }

    private val downloader = FileDownloader(application, client)
    
    val downloadHistory = logDao.getAllLogs()

    fun handleSharedUrl(url: String) {
        _externalUrl.value = url
    }

    fun consumeSharedUrl() {
        _externalUrl.value = null
    }

    fun fetchPreview(url: String, quality: String, mode: String = "auto", engine: String = "yt-dlp") {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    extractor.extract(url, quality, mode, engine)
                }
                _previewMetadata.value = result
            } catch (e: Exception) {
                _previewMetadata.value = null
            }
        }
    }

    fun clearPreview() {
        _previewMetadata.value = null
    }

    fun downloadMedia(url: String, quality: String, mode: String = "auto", engine: String = "yt-dlp") {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            _uiState.value = DownloadState.Downloading(0f)
            val notificationId = System.currentTimeMillis().toInt()
            lastNotificationId = notificationId
            
            try {
                val result = _previewMetadata.value ?: withContext(Dispatchers.IO) {
                    extractor.extract(url, quality, mode, engine)
                }
                
                if (result.status == "success") {
                    val downloadUrl = result.url ?: throw Exception("No download URL found")
                    val extension = result.ext ?: (if (engine == "gallery-dl") "jpg" else "mp4")
                    val title = result.title ?: "video"
                    val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
                    val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"
                    
                    downloader.downloadFile(
                        url = downloadUrl, 
                        fileName = fileName, 
                        relativePath = downloadPath.value,
                        customFolderUri = selectedFolderUri.value
                    ).collect { state ->
                        _uiState.value = state
                        if (state is DownloadState.Downloading) {
                            notificationHelper.showProgressNotification(notificationId, title, (state.progress * 100).toInt())
                        }
                        if (state is DownloadState.Success) {
                            val savedUri = Uri.parse(state.path)
                            downloader.finalizeVideo(savedUri)
                            logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = state.path))
                            
                            val folderUri = if (selectedFolderUri.value != null) {
                                Uri.parse(selectedFolderUri.value)
                            } else {
                                // Default Movies folder
                                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                Uri.parse("content://media/external/file/").buildUpon()
                                    .appendQueryParameter("path", moviesDir.absolutePath + "/" + downloadPath.value)
                                    .build()
                            }
                            notificationHelper.showProgressNotification(notificationId, title, 100, folderUri)
                        }
                    }
                } else {
                    val errorMsg = result.message ?: "Extraction failed"
                    _uiState.value = DownloadState.Error(errorMsg)
                    logDao.insertLog(DownloadLog(title = "Failed Download", url = url, status = "Error: $errorMsg"))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _uiState.value = DownloadState.Idle
                    lastNotificationId?.let { notificationHelper.cancelNotification(it) }
                } else {
                    val errorMsg = e.message ?: "Unknown error"
                    _uiState.value = DownloadState.Error(errorMsg)
                    logDao.insertLog(DownloadLog(title = "Failed Download", url = url, status = "Error: $errorMsg"))
                }
            }
        }
    }

    fun openSavedFolder() {
        val context = getApplication<Application>()
        val uri = if (selectedFolderUri.value != null) {
            Uri.parse(selectedFolderUri.value)
        } else {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            Uri.parse("content://media/external/file/").buildUpon()
                .appendQueryParameter("path", moviesDir.absolutePath + "/" + downloadPath.value)
                .build()
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(fallbackIntent) } catch (e2: Exception) {
                android.widget.Toast.makeText(context, "Could not open folder. Path: ${downloadPath.value}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        _uiState.value = DownloadState.Idle
        lastNotificationId?.let { notificationHelper.cancelNotification(it) }
    }

    fun deleteLog(log: DownloadLog) {
        viewModelScope.launch { logDao.deleteLog(log) }
    }

    fun clearAllLogs() {
        viewModelScope.launch { logDao.deleteAllLogs() }
    }

    fun resetState() {
        _uiState.value = DownloadState.Idle
    }
}
