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

enum class TerminalTheme(
    val displayName: String,
    val background: Long,
    val text: Long,
    val header: Long
) {
    CLASSIC("Classic Green", 0xFF0C0C0C, 0xFF00FF66, 0xFF1E1E1E),
    MOCHA("Mocha", 0xFF1E1E2E, 0xFFF5E0DC, 0xFF181825),
    PINK("Pink Sakura", 0xFF1F121F, 0xFFFF85A2, 0xFF2E1A2E),
    DRACULA("Dracula", 0xFF282A36, 0xFF50FA7B, 0xFF21222C)
}

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {
    private val extractor = PythonExtractor()
    private val cobaltRepository = CobaltRepository()
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "gabi_db").build()
    private val logDao = db.downloadLogDao()
    private val notificationHelper = NotificationHelper(application)
    
    private val prefs = application.getSharedPreferences("gabi_prefs", android.content.Context.MODE_PRIVATE)
    
    var terminalTheme = mutableStateOf(
        TerminalTheme.valueOf(prefs.getString("terminal_theme", TerminalTheme.PINK.name) ?: TerminalTheme.PINK.name)
    )
    
    fun updateTerminalTheme(theme: TerminalTheme) {
        terminalTheme.value = theme
        prefs.edit().putString("terminal_theme", theme.name).apply()
        logToConsole("Terminal theme changed to: ${theme.displayName}")
    }

    fun getDynamicEngine(url: String): String {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains("pinterest") || 
            lowerUrl.contains("pin.it") ||
            lowerUrl.contains("instagram.com") || 
            lowerUrl.contains("pixiv.net") || 
            lowerUrl.contains("deviantart.com") ||
            lowerUrl.contains("artstation.com") ||
            lowerUrl.contains("tumblr.com") ||
            lowerUrl.contains("flickr.com") -> "gallery-dl"
            
            else -> "yt-dlp"
        }
    }

    fun resetDownloadState() {
        _uiState.value = DownloadState.Idle
    }
    
    private val _consoleLogs = MutableStateFlow<List<String>>(listOf("gabi@terminal:~ $ app initialized"))
    val consoleLogs: StateFlow<List<String>> = _consoleLogs

    fun logToConsole(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _consoleLogs.value = _consoleLogs.value + "[$timestamp] $message"
    }

    fun clearConsole() {
        _consoleLogs.value = listOf("gabi@terminal:~ $ console cleared")
    }

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

    fun fetchPreview(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        viewModelScope.launch {
            val resolvedEngine = if (engine == "dynamic") getDynamicEngine(url) else engine
            logToConsole("Fetching preview: $url (Engine: $engine, Resolved: $resolvedEngine, Mode: $mode)")
            try {
                val result = withContext(Dispatchers.IO) {
                    if (resolvedEngine == "cobalt") {
                        val cobaltMode = if (mode == "video") "auto" else mode
                        logToConsole("Calling Cobalt API...")
                        val cobaltRes = cobaltRepository.fetchMediaLink(
                            url = url,
                            quality = quality,
                            downloadMode = cobaltMode
                        )
                        if (cobaltRes.status == "error") {
                            logToConsole("Cobalt API returned error: ${cobaltRes.text}")
                            ExtractionResult(status = "error", message = cobaltRes.text ?: "Cobalt error")
                        } else {
                            val resolvedUrl = cobaltRes.url ?: cobaltRes.picker?.firstOrNull()?.url
                            val path = resolvedUrl?.let { Uri.parse(it).path?.lowercase() } ?: ""
                            val detectedExt = when {
                                path.endsWith(".jpg") || path.endsWith(".jpeg") -> "jpg"
                                path.endsWith(".png") -> "png"
                                path.endsWith(".webp") -> "webp"
                                path.endsWith(".gif") -> "gif"
                                mode == "audio" -> "mp3"
                                else -> "mp4"
                            }
                            val isImage = detectedExt in listOf("jpg", "jpeg", "png", "webp", "gif")
                            logToConsole("Cobalt resolved URL successfully. Extracted format: $detectedExt")
                            
                            ExtractionResult(
                                status = "success",
                                url = resolvedUrl,
                                title = "Cobalt Download",
                                author = "Cobalt API",
                                thumbnail = if (isImage) resolvedUrl else cobaltRes.picker?.firstOrNull()?.thumb,
                                ext = detectedExt
                            )
                        }
                    } else {
                        logToConsole("Executing $resolvedEngine extractor in Python...")
                        val res = extractor.extract(url, quality, mode, resolvedEngine)
                        if (res.status == "success") {
                            if (res.is_gallery == true) {
                                logToConsole("Python extractor found gallery with ${res.urls?.size ?: 0} items")
                            } else {
                                logToConsole("Python extractor resolved URL successfully. Format: ${res.ext}")
                            }
                        } else {
                            logToConsole("Python extractor failed: ${res.message}")
                        }
                        res
                    }
                }
                if (result.status == "success") {
                    logToConsole("Preview loaded: '${result.title}' by ${result.author}")
                }
                _previewMetadata.value = result
            } catch (e: Exception) {
                logToConsole("Preview exception: ${e.message}")
                _previewMetadata.value = ExtractionResult(status = "error", message = e.message ?: "Extraction failed")
            }
        }
    }

    fun clearPreview() {
        _previewMetadata.value = null
        logToConsole("Cleared preview metadata")
    }

    fun downloadMedia(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            _uiState.value = DownloadState.Downloading(0f)
            val notificationId = System.currentTimeMillis().toInt()
            lastNotificationId = notificationId
            val resolvedEngine = if (engine == "dynamic") getDynamicEngine(url) else engine
            logToConsole("Starting download process for $url (Engine: $engine, Resolved: $resolvedEngine)")
            
            try {
                val result = _previewMetadata.value ?: withContext(Dispatchers.IO) {
                    logToConsole("No cached preview. Performing extraction first...")
                    if (resolvedEngine == "cobalt") {
                        val cobaltMode = if (mode == "video") "auto" else mode
                        val cobaltRes = cobaltRepository.fetchMediaLink(
                            url = url,
                            quality = quality,
                            downloadMode = cobaltMode
                        )
                        if (cobaltRes.status == "error") {
                            ExtractionResult(status = "error", message = cobaltRes.text ?: "Cobalt error")
                        } else {
                            val resolvedUrl = cobaltRes.url ?: cobaltRes.picker?.firstOrNull()?.url
                            val path = resolvedUrl?.let { Uri.parse(it).path?.lowercase() } ?: ""
                            val detectedExt = when {
                                path.endsWith(".jpg") || path.endsWith(".jpeg") -> "jpg"
                                path.endsWith(".png") -> "png"
                                path.endsWith(".webp") -> "webp"
                                path.endsWith(".gif") -> "gif"
                                mode == "audio" -> "mp3"
                                else -> "mp4"
                            }
                            val isImage = detectedExt in listOf("jpg", "jpeg", "png", "webp", "gif")
                            
                            ExtractionResult(
                                status = "success",
                                url = resolvedUrl,
                                title = "Cobalt Download",
                                author = "Cobalt API",
                                thumbnail = if (isImage) resolvedUrl else cobaltRes.picker?.firstOrNull()?.thumb,
                                ext = detectedExt
                            )
                        }
                    } else {
                        extractor.extract(url, quality, mode, resolvedEngine)
                    }
                }
                
                if (result.status == "success") {
                    val isGallery = result.is_gallery == true
                    val urlsToDownload = result.urls ?: listOf(result.url ?: throw Exception("No download URL found"))
                    val totalFiles = urlsToDownload.size
                    
                    if (isGallery && totalFiles > 1) {
                        logToConsole("Gallery download started: downloading $totalFiles files...")
                        var succeededCount = 0
                        for ((index, downloadUrl) in urlsToDownload.withIndex()) {
                            val extension = result.ext ?: "jpg"
                            val title = result.title ?: "gallery"
                            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
                            val fileName = "${sanitizedTitle}_${index + 1}_${System.currentTimeMillis()}.$extension"
                            
                            // Automatically save galleries to Pictures/Gabi instead of Movies/Gabi
                            val targetPath = if (downloadPath.value == "Movies/Gabi") "Pictures/Gabi" else downloadPath.value
                            logToConsole("Downloading file [${index + 1}/$totalFiles]: $fileName")
                            
                            downloader.downloadFile(
                                url = downloadUrl,
                                fileName = fileName,
                                relativePath = targetPath,
                                customFolderUri = selectedFolderUri.value
                            ).collect { state ->
                                if (state is DownloadState.Downloading) {
                                    val fileProgress = state.progress
                                    val overallProgress = (index + fileProgress) / totalFiles
                                    _uiState.value = DownloadState.Downloading(overallProgress)
                                    notificationHelper.showProgressNotification(notificationId, "$title (${index + 1}/$totalFiles)", (overallProgress * 100).toInt())
                                }
                                if (state is DownloadState.Success) {
                                    succeededCount++
                                    val savedUri = Uri.parse(state.path)
                                    downloader.finalizeFile(savedUri, fileName)
                                    logToConsole("File [${index + 1}/$totalFiles] saved successfully")
                                }
                                if (state is DownloadState.Error) {
                                    logToConsole("File [${index + 1}/$totalFiles] failed: ${state.message}")
                                }
                            }
                        }
                        
                        if (succeededCount > 0) {
                            _uiState.value = DownloadState.Success("")
                            logDao.insertLog(DownloadLog(title = "${result.title ?: "Gallery"} ($succeededCount images)", url = url, status = "Success"))
                            notificationHelper.showProgressNotification(notificationId, result.title ?: "Gallery", 100)
                            logToConsole("Gallery download complete. Successfully saved $succeededCount/$totalFiles files.")
                        } else {
                            throw Exception("Failed to download gallery images")
                        }
                    } else {
                        // Single file download
                        val downloadUrl = urlsToDownload.first()
                        val extension = result.ext ?: (if (resolvedEngine == "gallery-dl") "jpg" else "mp4")
                        val title = result.title ?: (if (resolvedEngine == "gallery-dl") "image" else "video")
                        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
                        val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"
                        
                        val targetPath = if ((resolvedEngine == "gallery-dl" || isGallery) && downloadPath.value == "Movies/Gabi") "Pictures/Gabi" else downloadPath.value
                        logToConsole("Downloading file: $fileName")
                        
                        downloader.downloadFile(
                            url = downloadUrl, 
                            fileName = fileName, 
                            relativePath = targetPath,
                            customFolderUri = selectedFolderUri.value
                        ).collect { state ->
                            _uiState.value = state
                            if (state is DownloadState.Downloading) {
                                notificationHelper.showProgressNotification(notificationId, title, (state.progress * 100).toInt())
                            }
                            if (state is DownloadState.Success) {
                                val savedUri = Uri.parse(state.path)
                                downloader.finalizeFile(savedUri, fileName)
                                logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = state.path))
                                logToConsole("File saved successfully to $targetPath/$fileName")
                                
                                val folderUri = if (selectedFolderUri.value != null) {
                                    Uri.parse(selectedFolderUri.value)
                                } else {
                                    val mediaDir = if (targetPath.startsWith("Pictures")) {
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                    } else {
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                    }
                                    Uri.parse("content://media/external/file/").buildUpon()
                                        .appendQueryParameter("path", mediaDir.absolutePath + "/" + targetPath)
                                        .build()
                                }
                                notificationHelper.showProgressNotification(notificationId, title, 100, folderUri)
                            }
                            if (state is DownloadState.Error) {
                                logToConsole("File download failed: ${state.message}")
                            }
                        }
                    }
                } else {
                    val errorMsg = result.message ?: "Extraction failed"
                    _uiState.value = DownloadState.Error(errorMsg)
                    logDao.insertLog(DownloadLog(title = "Failed Download", url = url, status = "Error: $errorMsg"))
                    logToConsole("Download failed: $errorMsg")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _uiState.value = DownloadState.Idle
                    lastNotificationId?.let { notificationHelper.cancelNotification(it) }
                    logToConsole("Download cancelled")
                } else {
                    val errorMsg = e.message ?: "Unknown error"
                    _uiState.value = DownloadState.Error(errorMsg)
                    logDao.insertLog(DownloadLog(title = "Failed Download", url = url, status = "Error: $errorMsg"))
                    logToConsole("Download exception: $errorMsg")
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
