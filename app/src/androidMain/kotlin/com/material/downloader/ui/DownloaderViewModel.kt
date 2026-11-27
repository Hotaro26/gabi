package com.material.downloader.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.material.downloader.api.CobaltRepository
import com.material.downloader.util.DownloadState
import com.material.downloader.util.FileDownloader
import com.material.downloader.util.GabiLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.material.downloader.api.PythonExtractor
import io.ktor.client.HttpClient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import androidx.room.Room
import com.material.downloader.db.AppDatabase
import com.material.downloader.model.DownloadLog
import com.material.downloader.model.CookieSession
import com.material.downloader.model.PlaylistItem
import com.material.downloader.model.PlaylistInfo
import com.material.downloader.model.CustomHeader
import com.material.downloader.model.QueueItem
import com.material.downloader.model.QueueItemStatus
import com.material.downloader.util.DownloadQueueManager
import kotlinx.serialization.encodeToString
import java.util.UUID
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
    DRACULA("Dracula", 0xFF282A36, 0xFFF8F8F2, 0xFF21222C)
}

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {
    private val extractor = PythonExtractor()
    private val cobaltRepository = CobaltRepository()
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "gabi_db")
        .addMigrations(com.material.downloader.db.MIGRATION_1_2, com.material.downloader.db.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()
    private val logDao = db.downloadLogDao()
    private val notificationHelper = NotificationHelper(application)

    private suspend fun saveThumbnailToCache(thumbnailUrl: String?, mediaPath: String?): String? = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val thumbDir = java.io.File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
            val thumbFile = java.io.File(thumbDir, "thumb_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
            
            // Remote image URL from preview
            if (!thumbnailUrl.isNullOrEmpty() && (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://"))) {
                java.net.URL(thumbnailUrl).openStream().use { input ->
                    thumbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext thumbFile.absolutePath
            }

            // Local video/media file frame extraction
            if (!mediaPath.isNullOrEmpty()) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    if (mediaPath.startsWith("content://") || mediaPath.startsWith("file://")) {
                        retriever.setDataSource(context, Uri.parse(mediaPath))
                    } else {
                        retriever.setDataSource(mediaPath)
                    }
                    val bitmap = retriever.frameAtTime
                    if (bitmap != null) {
                        thumbFile.outputStream().use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        return@withContext thumbFile.absolutePath
                    }
                } catch (e: Exception) {
                    // Ignore retriever failure
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext thumbnailUrl ?: mediaPath
    }
    
    private val prefs = application.getSharedPreferences("gabi_prefs", android.content.Context.MODE_PRIVATE)
    
    var terminalTheme = mutableStateOf(
        TerminalTheme.valueOf(prefs.getString("terminal_theme", TerminalTheme.MOCHA.name) ?: TerminalTheme.MOCHA.name)
    )
    
    var isNavBarBlurEnabled = mutableStateOf(prefs.getBoolean("nav_bar_blur", true))
    var isNavBarOpaque = mutableStateOf(prefs.getBoolean("nav_bar_opaque", false))
    var isNavBarTrueGlass = mutableStateOf(prefs.getBoolean("nav_bar_true_glass", false))
    var isOnboardingCompleted = mutableStateOf(prefs.getBoolean("onboarding_completed", false))
    var useCookies = mutableStateOf(prefs.getBoolean("use_cookies", true))
    var isMaterialShapesEnabled = mutableStateOf(prefs.getBoolean("material_shapes", false))
    fun toggleNavBarBlur(enabled: Boolean) {
        isNavBarBlurEnabled.value = enabled
        prefs.edit().putBoolean("nav_bar_blur", enabled).apply()
    }
    
    fun setOnboardingCompleted() {
        isOnboardingCompleted.value = true
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun toggleNavBarOpaque(enabled: Boolean) {
        isNavBarOpaque.value = enabled
        prefs.edit().putBoolean("nav_bar_opaque", enabled).apply()
    }
    
    fun toggleNavBarTrueGlass(enabled: Boolean) {
        isNavBarTrueGlass.value = enabled
        prefs.edit().putBoolean("nav_bar_true_glass", enabled).apply()
    }
    
    fun toggleUseCookies(enabled: Boolean) {
        useCookies.value = enabled
        prefs.edit().putBoolean("use_cookies", enabled).apply()
    }
    
    fun toggleMaterialShapes(enabled: Boolean) {
        isMaterialShapesEnabled.value = enabled
        prefs.edit().putBoolean("material_shapes", enabled).apply()
    }    
    fun updateTerminalTheme(theme: TerminalTheme) {
        terminalTheme.value = theme
        prefs.edit().putString("terminal_theme", theme.name).apply()
        logToConsole("Terminal theme changed to: ${theme.displayName}")
    }

    
    private val _cookieSessions = MutableStateFlow<List<CookieSession>>(emptyList())
    val cookieSessions: StateFlow<List<CookieSession>> = _cookieSessions

    private fun loadCookieSessions() {
        val jsonStr = prefs.getString("cookie_sessions", "[]") ?: "[]"
        try {
            val sessions = kotlinx.serialization.json.Json.decodeFromString<List<CookieSession>>(jsonStr)
            _cookieSessions.value = sessions
        } catch (e: Exception) {
            _cookieSessions.value = emptyList()
        }
    }

    fun addCookieSession(session: CookieSession) {
        val newList = _cookieSessions.value + session
        saveCookieSessions(newList)
    }

    fun removeCookieSession(id: String) {
        val newList = _cookieSessions.value.filter { it.id != id }
        saveCookieSessions(newList)
    }

    fun reorderCookieSessions(from: Int, to: Int) {
        val list = _cookieSessions.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        saveCookieSessions(list)
    }

    private fun saveCookieSessions(sessions: List<CookieSession>) {
        _cookieSessions.value = sessions
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(sessions)
        prefs.edit().putString("cookie_sessions", jsonStr).apply()
        regenerateCookiesTxt("yt_dlp")
        regenerateCookiesTxt("gallery_dl")
    }
    
    private fun regenerateCookiesTxt(engine: String) {
        val context = getApplication<Application>()
        val file = java.io.File(context.filesDir, "${engine}_cookies.txt")
        val sessions = _cookieSessions.value.filter { it.engine == engine }
        
        if (sessions.isEmpty()) {
            if (file.exists()) file.delete()
            saveSetting("${engine}_cookies_path", "")
            return
        }
        
        val out = StringBuilder()
        out.append("# Netscape HTTP Cookie File\n\n")
        
        for (session in sessions) {
            val cookies = session.cookieString.split(";")
            for (cookie in cookies) {
                val parts = cookie.trim().split("=", limit = 2)
                if (parts.size == 2) {
                    val name = parts[0]
                    val value = parts[1]
                    val domainWithDot = if (session.domain.startsWith(".")) session.domain else ".${session.domain}"
                    out.append("$domainWithDot\tTRUE\t/\tFALSE\t2147483647\t$name\t$value\n")
                }
            }
        }
        file.writeText(out.toString())
        saveSetting("${engine}_cookies_path", file.absolutePath)
    }

    // Custom Network Headers
    private val _customHeaders = MutableStateFlow<List<CustomHeader>>(emptyList())
    val customHeaders: StateFlow<List<CustomHeader>> = _customHeaders

    private fun loadCustomHeaders() {
        val jsonStr = prefs.getString("custom_headers", "[]") ?: "[]"
        try {
            _customHeaders.value = kotlinx.serialization.json.Json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            _customHeaders.value = emptyList()
        }
    }

    fun addCustomHeader(key: String, value: String) {
        val cleanKey = key.trim()
        val cleanVal = value.trim()
        if (cleanKey.isEmpty() || cleanVal.isEmpty()) return
        val newHeader = CustomHeader(
            id = UUID.randomUUID().toString(),
            key = cleanKey,
            value = cleanVal,
            isEnabled = true
        )
        val updated = _customHeaders.value + newHeader
        saveCustomHeaders(updated)
        logToConsole("Added custom header: $cleanKey")
    }

    fun removeCustomHeader(id: String) {
        val updated = _customHeaders.value.filter { it.id != id }
        saveCustomHeaders(updated)
    }

    fun toggleCustomHeader(id: String) {
        val updated = _customHeaders.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveCustomHeaders(updated)
    }

    fun applyHeaderPreset(preset: String) {
        val presetHeaders = when (preset) {
            "Chrome Desktop" -> listOf(
                CustomHeader(UUID.randomUUID().toString(), "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            )
            "Mobile Safari (iOS)" -> listOf(
                CustomHeader(UUID.randomUUID().toString(), "User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1")
            )
            "Firefox Desktop" -> listOf(
                CustomHeader(UUID.randomUUID().toString(), "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0")
            )
            "Android Mobile" -> listOf(
                CustomHeader(UUID.randomUUID().toString(), "User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.113 Mobile Safari/537.36")
            )
            else -> emptyList()
        }
        val nonUa = _customHeaders.value.filter { !it.key.equals("User-Agent", ignoreCase = true) }
        saveCustomHeaders(nonUa + presetHeaders)
        logToConsole("Applied header preset: $preset")
    }

    private fun saveCustomHeaders(list: List<CustomHeader>) {
        _customHeaders.value = list
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(list)
        prefs.edit().putString("custom_headers", jsonStr).apply()
    }

    fun getCustomHeadersMap(): Map<String, String> {
        return _customHeaders.value.filter { it.isEnabled }.associate { it.key to it.value }
    }

    fun getCustomHeadersJson(): String {
        return kotlinx.serialization.json.Json.encodeToString(_customHeaders.value.filter { it.isEnabled })
    }

    fun isPlaylistUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.contains("list=") || 
               lower.contains("/playlist") || 
               lower.contains("/sets/") || 
               lower.contains("/album/") || 
               lower.contains("/albums/")
    }

    fun getDynamicEngine(url: String): String {
        val lowerUrl = url.trim().lowercase()
        if (isPlaylistUrl(lowerUrl)) {
            return "yt-dlp"
        }
        return when {
            lowerUrl.contains("pinterest") || 
            lowerUrl.contains("pin.it") ||
            lowerUrl.contains("instagram.com") || 
            lowerUrl.contains("pixiv.net") || 
            lowerUrl.contains("deviantart.com") ||
            lowerUrl.contains("artstation.com") ||
            lowerUrl.contains("tumblr.com") ||
            lowerUrl.contains("flickr.com") -> "gallery-dl"
            
            lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("soundcloud.com") -> "newpipe"
            
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
        com.material.downloader.util.GabiLogger.i(message)
    }

    fun clearConsole() {
        _consoleLogs.value = listOf("gabi@terminal:~ $ console cleared")
    }

    fun saveSetting(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getSetting(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    suspend fun hasDownloadedUrl(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            logDao.hasUrl(url) > 0
        }
    }

    var autoCheckUpdates = mutableStateOf(prefs.getBoolean("auto_check_updates", true))
    var updateAvailable = mutableStateOf<Pair<String, String>?>(null)
    var updateDownloadProgress = mutableStateOf(-1f)
    var isCheckingUpdates = mutableStateOf(false)
    var updateCheckMessage = mutableStateOf<String?>(null)

    data class ExtractorUpdateInfo(
        val hasUpdate: Boolean,
        val ytdlpCurrent: String,
        val ytdlpLatest: String?,
        val gallerydlCurrent: String,
        val gallerydlLatest: String?
    )

    var extractorUpdateAvailable = mutableStateOf<ExtractorUpdateInfo?>(null)
    var showExtractorUpdateDialog = mutableStateOf(false)
    var isCheckingExtractorUpdates = mutableStateOf(false)
    var isUpdatingExtractors = mutableStateOf(false)

    fun downloadUpdate(context: android.content.Context, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateDownloadProgress.value = 0f
                val connection = java.net.URL(url).openConnection()
                connection.connect()
                
                val fileLength = connection.contentLength
                val input = connection.getInputStream()
                
                val fileName = "update.apk"
                val apkFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                val output = java.io.FileOutputStream(apkFile)
                
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        updateDownloadProgress.value = total.toFloat() / fileLength.toFloat()
                    }
                    output.write(data, 0, count)
                }
                
                output.flush()
                output.close()
                input.close()
                
                updateDownloadProgress.value = -1f
                updateAvailable.value = null
                withContext(Dispatchers.Main) {
                    com.material.downloader.util.AppUpdater(context).installApk(fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateDownloadProgress.value = -1f
                    android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        autoCheckUpdates.value = enabled
        prefs.edit().putBoolean("auto_check_updates", enabled).apply()
        logToConsole("Auto version check ${if (enabled) "enabled" else "disabled"}")
    }

    private val client = HttpClient(io.ktor.client.engine.android.Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }

    }

    private val downloader = FileDownloader(application, client)

    private var currentDownloadJob: Job? = null
    private var lastNotificationId: Int? = null

    private val activeTempFiles = java.util.Collections.synchronizedList(mutableListOf<java.io.File>())
    private val activeDownloadUris = java.util.Collections.synchronizedList(mutableListOf<Uri>())

    private val _uiState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val uiState: StateFlow<DownloadState> = _uiState

    private val _previewMetadata = MutableStateFlow<ExtractionResult?>(null)
    val previewMetadata: StateFlow<ExtractionResult?> = _previewMetadata

    // Playlist State
    private val _playlistInfo = MutableStateFlow<PlaylistInfo?>(null)
    val playlistInfo: StateFlow<PlaylistInfo?> = _playlistInfo

    // Parallel Download Queue
    var maxParallelDownloads = mutableIntStateOf(prefs.getInt("max_parallel_downloads", 2))

    val queueManager: DownloadQueueManager by lazy {
        DownloadQueueManager(
            scope = viewModelScope,
            downloadWorker = { item, onProgress ->
                executeSingleQueueItem(item, onProgress)
            }
        ).apply {
            setMaxConcurrency(maxParallelDownloads.intValue)
        }
    }

    val queueItems: StateFlow<List<QueueItem>> get() = queueManager.queue

    fun updateMaxParallelDownloads(count: Int) {
        val safe = count.coerceIn(1, 5)
        maxParallelDownloads.intValue = safe
        prefs.edit().putInt("max_parallel_downloads", safe).apply()
        queueManager.setMaxConcurrency(safe)
        logToConsole("Max concurrent downloads set to $safe")
    }

    init {
        loadCookieSessions()
        loadCustomHeaders()

        // Sync queue active downloads with _uiState so standard UI continues reacting smoothly
        viewModelScope.launch {
            queueManager.queue.collect { list ->
                val active = list.find { it.status == QueueItemStatus.DOWNLOADING }
                if (active != null) {
                    _uiState.value = DownloadState.Downloading(
                        progress = active.progress,
                        downloadedBytes = active.downloadedBytes,
                        totalBytes = active.totalBytes,
                        speedBps = active.speedBps
                    )
                } else {
                    val hasCompleted = list.any { it.status == QueueItemStatus.COMPLETED }
                    val hasError = list.any { it.status == QueueItemStatus.ERROR }
                    if (_uiState.value is DownloadState.Downloading) {
                        if (hasCompleted) {
                            _uiState.value = DownloadState.Success("")
                        } else if (hasError) {
                            _uiState.value = DownloadState.Error("Queue finished with errors")
                        } else {
                            _uiState.value = DownloadState.Idle
                        }
                    }
                }
            }
        }

        if (autoCheckUpdates.value) {
            checkForUpdates(manual = false)
            checkExtractorUpdates(manual = false)
        }
    }

    fun checkExtractorUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            isCheckingExtractorUpdates.value = true
            try {
                val (hasYtUpdate, hasGalUpdate, info) = withContext(Dispatchers.IO) {
                    val versions = extractor.getVersions()
                    val curYt = versions["yt_dlp"] ?: ""
                    val curGal = versions["gallery_dl"] ?: ""

                    var latestYt: String? = null
                    var latestGal: String? = null

                    try {
                        val ytResp: HttpResponse = client.get("https://pypi.org/pypi/yt-dlp/json")
                        if (ytResp.status.value in 200..299) {
                            val body = ytResp.bodyAsText()
                            val match = "\"version\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(body)
                            latestYt = match?.groupValues?.getOrNull(1)
                        }
                    } catch (e: Exception) {
                        GabiLogger.e("DownloaderViewModel", "yt-dlp pypi check failed: ${e.message}")
                    }

                    try {
                        val galResp: HttpResponse = client.get("https://pypi.org/pypi/gallery-dl/json")
                        if (galResp.status.value in 200..299) {
                            val body = galResp.bodyAsText()
                            val match = "\"version\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(body)
                            latestGal = match?.groupValues?.getOrNull(1)
                        }
                    } catch (e: Exception) {
                        GabiLogger.e("DownloaderViewModel", "gallery-dl pypi check failed: ${e.message}")
                    }

                    val hasYt = isNewerVersion(latestYt, curYt)
                    val hasGal = isNewerVersion(latestGal, curGal)

                    val resInfo = if (hasYt || hasGal) {
                        ExtractorUpdateInfo(
                            hasUpdate = true,
                            ytdlpCurrent = curYt,
                            ytdlpLatest = latestYt,
                            gallerydlCurrent = curGal,
                            gallerydlLatest = latestGal
                        )
                    } else null

                    Triple(hasYt, hasGal, resInfo)
                }

                if (info != null) {
                    extractorUpdateAvailable.value = info
                    showExtractorUpdateDialog.value = true
                    logToConsole("Engine update available: yt-dlp (${info.ytdlpCurrent} -> ${info.ytdlpLatest}), gallery-dl (${info.gallerydlCurrent} -> ${info.gallerydlLatest})")
                } else if (manual) {
                    logToConsole("Engines are up to date")
                }
            } catch (e: Exception) {
                GabiLogger.e("DownloaderViewModel", "checkExtractorUpdates error: ${e.message}", e)
            } finally {
                isCheckingExtractorUpdates.value = false
            }
        }
    }

    private fun isNewerVersion(latest: String?, current: String?): Boolean {
        if (latest.isNullOrBlank() || current.isNullOrBlank()) return false
        if (latest == current) return false
        val cleanL = latest.trim().removePrefix("v").substringBefore("-")
        val cleanC = current.trim().removePrefix("v").substringBefore("-")
        if (cleanL == cleanC) return false
        val lParts = cleanL.split('.').map { it.toIntOrNull() ?: 0 }
        val cParts = cleanC.split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(lParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val l = lParts.getOrNull(i) ?: 0
            val c = cParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun updateExtractors(context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isUpdatingExtractors.value = true
            try {
                val targetPath = java.io.File(context.filesDir, "python_packages").absolutePath
                val result = extractor.updateExtractors(targetPath)
                val success = result["status"] == "success"
                withContext(Dispatchers.Main) {
                    isUpdatingExtractors.value = false
                    if (success) {
                        extractorUpdateAvailable.value = null
                        showExtractorUpdateDialog.value = false
                    }
                    onComplete(success, result["message"] ?: "")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isUpdatingExtractors.value = false
                    onComplete(false, e.message ?: "Failed to update")
                }
            }
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdates.value = true
            updateCheckMessage.value = if (manual) "Checking for updates..." else null
            try {
                val response: HttpResponse = client.get("https://api.github.com/repos/Hotaro26/gabi/releases/latest")
                if (response.status.value in 200..299) {
                    val responseBody = response.bodyAsText()
                    val tagRegex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val urlRegex = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"".toRegex()
                    val match = tagRegex.find(responseBody)
                    val urlMatch = urlRegex.find(responseBody)
                    if (match != null) {
                        val latestVersion = match.groupValues[1]
                        val apkUrl = urlMatch?.groupValues?.getOrNull(1) ?: "https://github.com/Hotaro26/gabi/releases/latest"
                        val currentVersion = "v" + com.material.downloader.BuildConfig.VERSION_NAME
                        
                        val cleanLatest = latestVersion.substringBefore("-").removePrefix("v").trim()
                        val cleanCurrent = com.material.downloader.BuildConfig.VERSION_NAME.substringBefore("-").removePrefix("v").trim()
                        
                        var isNewer = false
                        try {
                            val lParts = cleanLatest.split('.').map { it.toIntOrNull() ?: 0 }
                            val cParts = cleanCurrent.split('.').map { it.toIntOrNull() ?: 0 }
                            val maxLen = maxOf(lParts.size, cParts.size)
                            for (i in 0 until maxLen) {
                                val l = lParts.getOrNull(i) ?: 0
                                val c = cParts.getOrNull(i) ?: 0
                                if (l > c) {
                                    isNewer = true
                                    break
                                } else if (l < c) {
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback if parsing fails
                            isNewer = latestVersion != currentVersion && !latestVersion.contains("beta", ignoreCase = true)
                        }

                        if (isNewer && !latestVersion.contains("beta", ignoreCase = true)) {
                            updateAvailable.value = Pair(latestVersion, apkUrl)
                            updateCheckMessage.value = "New version $latestVersion available!"
                        } else {
                            if (manual) {
                                updateCheckMessage.value = "Up to date ($currentVersion)"
                            }
                        }
                    } else if (manual) {
                        updateCheckMessage.value = "No releases found."
                    }
                } else if (manual) {
                    updateCheckMessage.value = "Check failed (HTTP ${response.status.value})"
                }
            } catch (e: Exception) {
                logToConsole("Update check failed: ${e.message}")
                if (manual) {
                    updateCheckMessage.value = "Check failed: ${e.message}"
                }
            } finally {
                isCheckingUpdates.value = false
            }
        }
    }

    fun togglePlaylistItem(itemId: String) {
        _playlistInfo.value?.let { current ->
            val updated = current.items.map {
                if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
            }
            _playlistInfo.value = current.copy(items = updated)
        }
    }

    fun selectAllPlaylistItems(selected: Boolean) {
        _playlistInfo.value?.let { current ->
            val updated = current.items.map { it.copy(isSelected = selected) }
            _playlistInfo.value = current.copy(items = updated)
        }
    }

    fun dismissPlaylist() {
        _playlistInfo.value = null
    }

    fun enqueueDownload(
        url: String, 
        quality: String = "720", 
        mode: String = "auto", 
        engine: String = "dynamic", 
        title: String? = null, 
        thumbnail: String? = null
    ) {
        val resolvedTitle = title ?: _previewMetadata.value?.title ?: "Download"
        val resolvedThumb = thumbnail ?: _previewMetadata.value?.thumbnail
        val item = QueueItem(
            id = UUID.randomUUID().toString(),
            url = url,
            title = resolvedTitle,
            author = _previewMetadata.value?.author,
            thumbnail = resolvedThumb,
            quality = quality,
            mode = mode,
            engine = engine
        )
        queueManager.enqueue(item)
        logToConsole("Added to download queue: $resolvedTitle")
    }

    fun enqueueSelectedPlaylist(quality: String = "720", mode: String = "auto", engine: String = "dynamic") {
        val current = _playlistInfo.value ?: return
        val selected = current.items.filter { it.isSelected }
        if (selected.isEmpty()) return

        val items = selected.map { track ->
            QueueItem(
                id = UUID.randomUUID().toString(),
                url = track.url,
                title = track.title,
                author = track.author ?: current.author,
                thumbnail = track.thumbnail ?: current.thumbnail,
                quality = quality,
                mode = mode,
                engine = engine,
                playlistId = current.id
            )
        }
        queueManager.enqueueBatch(items)
        logToConsole("Enqueued ${items.size} tracks from playlist '${current.title}'")
        _playlistInfo.value = null
    }

    fun pauseQueueItem(id: String) = queueManager.pause(id)
    fun resumeQueueItem(id: String) = queueManager.resume(id)
    fun cancelQueueItem(id: String) = queueManager.cancel(id)
    fun retryQueueItem(id: String) = queueManager.retry(id)
    fun clearCompletedQueue() = queueManager.clearCompleted()
    fun clearAllQueue() = queueManager.clearAll()

    var downloadPath = mutableStateOf("Download/Gabi")
    var selectedFolderUri = mutableStateOf<String?>(null)
    var selectedFolderName = mutableStateOf<String?>(null)

    var newPipeQuery = mutableStateOf("")
    var newPipeResults = mutableStateOf<List<org.schabi.newpipe.extractor.stream.StreamInfoItem>>(emptyList())
    var triggerNewPipeSearch = mutableStateOf(false)

    var isNewPipeLoading = mutableStateOf(false)
    fun performNewPipeSearch(query: String) {
        if (query.isNotBlank()) {
            isNewPipeLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val searchExtractor = org.schabi.newpipe.extractor.ServiceList.YouTube.getSearchExtractor(query)
                    searchExtractor.fetchPage()
                    val items = searchExtractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                    withContext(Dispatchers.Main) {
                        newPipeResults.value = items
                        isNewPipeLoading.value = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isNewPipeLoading.value = false
                    }
                }
            }
        }
    }

    val voiceSearchEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

    // Appearance Settings
    var themeMode = mutableIntStateOf(prefs.getInt("theme_mode", 0))
    var selectedTheme = mutableStateOf(AppTheme.valueOf(prefs.getString("app_theme", AppTheme.Dynamic.name) ?: AppTheme.Dynamic.name))

    fun updateThemeMode(mode: Int) {
        themeMode.intValue = mode
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    fun updateSelectedTheme(theme: AppTheme) {
        selectedTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }
    
    // External Share Support
    private val _externalUrl = MutableStateFlow<String?>(null)
    val externalUrl: StateFlow<String?> = _externalUrl

    val downloadHistory = logDao.getAllLogs()

    fun handleSharedUrl(url: String) {
        _externalUrl.value = url
    }

    fun consumeSharedUrl() {
        _externalUrl.value = null
    }

    private suspend fun performExtraction(url: String, quality: String, mode: String, engine: String): ExtractionResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (isPlaylistUrl(url)) {
                logToConsole("Playlist URL detected. Querying Python playlist extractor...")
                val engineKey = "yt_dlp"
                val cookiesPath = if (useCookies.value) prefs.getString("${engineKey}_cookies_path", null) else null
                val webViewUa = if (useCookies.value) android.webkit.WebSettings.getDefaultUserAgent(getApplication()) else null
                val customHeadersJson = getCustomHeadersJson()
                val res = extractor.extractPlaylist(url, cookiesPath, webViewUa, customHeadersJson)
                if (res.status == "success") {
                    logToConsole("Playlist extraction success: ${res.playlist_items?.size ?: 0} tracks found")
                } else {
                    logToConsole("Playlist extraction failed: ${res.message}")
                }
                return@withContext res
            }

            if (engine == "cobalt") {
                val cobaltMode = if (mode == "video") "auto" else mode
                logToConsole("Calling Cobalt API...")
                val cobaltRes = cobaltRepository.fetchMediaLink(
                    url = url,
                    quality = quality,
                    downloadMode = cobaltMode
                )
                if (cobaltRes.status == "error") {
                    logToConsole("Cobalt API returned error: ${cobaltRes.text}")
                    com.material.downloader.api.ExtractionResult(status = "error", message = cobaltRes.text ?: "Cobalt error")
                } else {
                    val resolvedUrl = cobaltRes.url ?: cobaltRes.picker?.firstOrNull()?.url
                    val path = resolvedUrl?.let { android.net.Uri.parse(it).path?.lowercase() } ?: ""
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
                    
                    com.material.downloader.api.ExtractionResult(
                        status = "success",
                        url = resolvedUrl,
                        title = cobaltRes.filename ?: "Cobalt Download",
                        author = "Cobalt API",
                        thumbnail = cobaltRes.picker?.firstOrNull()?.thumb ?: resolvedUrl,
                        ext = detectedExt
                    )
                }
            } else if (engine == "newpipe") {
                logToConsole("Executing NewPipeExtractor...")
                try {
                    val service = org.schabi.newpipe.extractor.NewPipe.getServiceByUrl(url)
                    val urlObj = service.getStreamExtractor(url)
                    urlObj.fetchPage()
                    
                    val videoOnlyStreams = urlObj.videoOnlyStreams.filter {
                        it.isUrl && !it.content.isNullOrBlank() &&
                        (it.codec?.contains("av01", ignoreCase = true) != true)
                    }
                    val progressiveStreams = urlObj.videoStreams.filter { it.isUrl && !it.content.isNullOrBlank() }
                    val validAudioStreams = urlObj.audioStreams.filter { it.isUrl && !it.content.isNullOrBlank() }
                    val qualityClean = quality.replace("p", "").trim()
                    val isBestQuality = quality.equals("best", ignoreCase = true) ||
                        quality.equals("max", ignoreCase = true) ||
                        quality.isBlank()

                    val selectedVideo = if (mode == "audio") {
                        null
                    } else {
                        // Prefer mp4 (H.264) streams first for maximum Android MediaMuxer compatibility
                        val mp4VideoOnly = videoOnlyStreams.filter { it.getFormat()?.suffix.equals("mp4", ignoreCase = true) }
                        val poolVideoOnly = if (mp4VideoOnly.isNotEmpty()) mp4VideoOnly else videoOnlyStreams

                        val matchedVideoOnly = if (isBestQuality) {
                            poolVideoOnly.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        } else {
                            poolVideoOnly.find { it.resolution.contains(qualityClean) }
                                ?: poolVideoOnly.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        }
                        
                        val matchedProgressive = if (isBestQuality) {
                            progressiveStreams.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        } else {
                            progressiveStreams.find { it.resolution.contains(qualityClean) }
                                ?: progressiveStreams.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        }
                        
                        val videoOnlyRes = matchedVideoOnly?.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        val progressiveRes = matchedProgressive?.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        
                        if (videoOnlyRes > progressiveRes) {
                            matchedVideoOnly
                        } else {
                            matchedProgressive ?: matchedVideoOnly
                        }
                    }

                    val targetVideoExt = selectedVideo?.getFormat()?.suffix?.lowercase() ?: "mp4"
                    val compatibleAudioStreams = if (mode != "audio" && targetVideoExt == "webm") {
                        val webmAudio = validAudioStreams.filter { it.getFormat()?.suffix.equals("webm", ignoreCase = true) }
                        if (webmAudio.isNotEmpty()) webmAudio else validAudioStreams
                    } else {
                        val m4aAudio = validAudioStreams.filter { it.getFormat()?.suffix.equals("m4a", ignoreCase = true) }
                        if (m4aAudio.isNotEmpty()) m4aAudio else validAudioStreams
                    }
                    val englishAudio = compatibleAudioStreams.filter { it.audioLocale?.language == "en" }
                    val defaultAudio = compatibleAudioStreams.filter { it.audioLocale == null }
                    
                    val bestAudio = englishAudio.maxByOrNull { it.averageBitrate }
                        ?: defaultAudio.maxByOrNull { it.averageBitrate }
                        ?: compatibleAudioStreams.maxByOrNull { it.averageBitrate }
                        ?: validAudioStreams.maxByOrNull { it.averageBitrate }

                    val isMuxingRequired = selectedVideo != null && videoOnlyStreams.contains(selectedVideo)
                    
                    val streamUrl = if (mode == "audio") bestAudio?.content else selectedVideo?.content ?: bestAudio?.content
                    val audioUrl = if (isMuxingRequired) bestAudio?.content else null
                    val ext = if (mode == "audio") "m4a" else selectedVideo?.getFormat()?.suffix ?: "mp4"
                    
                    val availableQuals = (videoOnlyStreams + progressiveStreams)
                        .mapNotNull { it.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() }
                        .distinct()
                        .sortedDescending()
                        .map { "${it}p" }
                    
                    val maxResInt = (videoOnlyStreams + progressiveStreams)
                        .mapNotNull { it.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() }
                        .maxOrNull()
                    val maxResStr = if (maxResInt != null) "${maxResInt}p" else null

                    if (streamUrl != null) {
                        com.material.downloader.api.ExtractionResult(
                            status = "success",
                            url = streamUrl,
                            audio_url = audioUrl,
                            title = urlObj.name,
                            author = urlObj.uploaderName,
                            thumbnail = urlObj.thumbnails?.firstOrNull()?.url ?: "",
                            ext = ext,
                            max_resolution = maxResStr,
                            available_qualities = availableQuals
                        )
                    } else {
                        com.material.downloader.api.ExtractionResult(status = "error", message = "No streams found")
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: "NewPipe extraction failed"
                    logToConsole("NewPipe extractor failed: $msg")
                    com.material.downloader.api.ExtractionResult(status = "error", message = msg)
                }
            } else {
                logToConsole("Executing $engine extractor in Python...")
                val engineKey = if (engine == "gallery-dl") "gallery_dl" else "yt_dlp"
                val cookiesPath = if (useCookies.value) prefs.getString("${engineKey}_cookies_path", null) else null
                val webViewUa = if (useCookies.value) android.webkit.WebSettings.getDefaultUserAgent(getApplication()) else null
                val customHeadersJson = getCustomHeadersJson()
                val res = extractor.extract(url, quality, mode, engine, cookiesPath, webViewUa, customHeadersJson)
                if (res.status == "success") {
                    if (res.is_playlist == true) {
                        logToConsole("Python extractor found playlist with ${res.playlist_items?.size ?: 0} items")
                    } else if (res.is_gallery == true) {
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
    }

    fun fetchPreview(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        viewModelScope.launch {
            clearConsole()
            logToConsole("----------------------------------------")
            logToConsole("[SYSTEM] Fetching video metadata...")
            logToConsole("[INFO] URL: $url")
            logToConsole("[INFO] Preferred Quality: ${quality}p | Engine: $engine")
            logToConsole("----------------------------------------")
            try {
                var result: com.material.downloader.api.ExtractionResult? = null
                val enginesToTry = if (isPlaylistUrl(url)) {
                    listOf("yt-dlp")
                } else if (engine == "dynamic") {
                    val firstEngine = getDynamicEngine(url)
                    val allEngines = listOf("cobalt", "yt-dlp", "gallery-dl", "newpipe")
                    listOf(firstEngine) + allEngines.filter { it != firstEngine }
                } else {
                    listOf(engine)
                }

                for (currentEngine in enginesToTry) {
                    logToConsole("[SYSTEM] Querying extractor: $currentEngine...")
                    val currentResult = performExtraction(url, quality, mode, currentEngine)
                    if (currentResult.status == "success") {
                        result = currentResult
                        break
                    } else {
                        logToConsole("[WARNING] Extractor $currentEngine failed (${currentResult.message}). ${if (enginesToTry.last() != currentEngine) "Trying next candidate..." else "All candidates exhausted."}")
                        result = currentResult
                    }
                }

                if (result?.status == "success") {
                    logToConsole("[SUCCESS] Metadata resolved successfully!")
                    logToConsole("[INFO] Title: '${result?.title}'")
                    logToConsole("[INFO] Author: ${result?.author}")
                    val playlistItems = result.playlist_items
                    if (result.is_playlist == true && !playlistItems.isNullOrEmpty()) {
                        logToConsole("[INFO] Detected Playlist with ${playlistItems.size} tracks!")
                        _playlistInfo.value = PlaylistInfo(
                            id = result.playlist_id ?: "playlist_${System.currentTimeMillis()}",
                            title = result.playlist_title ?: result.title ?: "Playlist",
                            author = result.author,
                            thumbnail = result.thumbnail,
                            itemCount = playlistItems.size,
                            items = playlistItems
                        )
                    }
                    result?.max_resolution?.let { maxRes ->
                        logToConsole("Highest available resolution: $maxRes")
                    }
                }
                _previewMetadata.value = result
            } catch (e: Exception) {
                logToConsole("Preview exception: ${e.message}")
                _previewMetadata.value = com.material.downloader.api.ExtractionResult(status = "error", message = e.message ?: "Extraction failed")
            }
        }
    }

    fun clearPreview() {
        _previewMetadata.value = null
        logToConsole("Cleared preview metadata")
    }

    fun setPreviewMetadata(result: com.material.downloader.api.ExtractionResult) {
        _previewMetadata.value = result
    }

    private fun muxVideoAudio(videoFile: java.io.File, audioFile: java.io.File, outputFile: java.io.File) {
        val videoExtractor = android.media.MediaExtractor()
        videoExtractor.setDataSource(videoFile.absolutePath)
        
        val audioExtractor = android.media.MediaExtractor()
        audioExtractor.setDataSource(audioFile.absolutePath)
        
        val format = if (outputFile.absolutePath.endsWith(".webm", ignoreCase = true)) {
            android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        } else {
            android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }
        val muxer = android.media.MediaMuxer(outputFile.absolutePath, format)
        
        var videoTrackIndex = -1
        for (i in 0 until videoExtractor.trackCount) {
            val format = videoExtractor.getTrackFormat(i)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/")) {
                videoExtractor.selectTrack(i)
                videoTrackIndex = muxer.addTrack(format)
                break
            }
        }
        
        var audioTrackIndex = -1
        for (i in 0 until audioExtractor.trackCount) {
            val format = audioExtractor.getTrackFormat(i)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioExtractor.selectTrack(i)
                audioTrackIndex = muxer.addTrack(format)
                break
            }
        }
        
        if (videoTrackIndex == -1) {
            videoExtractor.release()
            audioExtractor.release()
            throw java.io.IOException("Video track not found in source video stream")
        }
        if (audioTrackIndex == -1) {
            videoExtractor.release()
            audioExtractor.release()
            throw java.io.IOException("Audio track not found in source audio stream")
        }

        muxer.start()
        
        val videoBuffer = java.nio.ByteBuffer.allocate(1024 * 1024)
        val videoBufferInfo = android.media.MediaCodec.BufferInfo()
        while (true) {
            videoBufferInfo.offset = 0
            videoBufferInfo.size = videoExtractor.readSampleData(videoBuffer, 0)
            if (videoBufferInfo.size < 0) {
                break
            }
            videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
            videoBufferInfo.flags = videoExtractor.sampleFlags
            muxer.writeSampleData(videoTrackIndex, videoBuffer, videoBufferInfo)
            videoExtractor.advance()
        }
        
        val audioBuffer = java.nio.ByteBuffer.allocate(1024 * 1024)
        val audioBufferInfo = android.media.MediaCodec.BufferInfo()
        while (true) {
            audioBufferInfo.offset = 0
            audioBufferInfo.size = audioExtractor.readSampleData(audioBuffer, 0)
            if (audioBufferInfo.size < 0) {
                break
            }
            audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
            audioBufferInfo.flags = audioExtractor.sampleFlags
            muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
            audioExtractor.advance()
        }
        
        muxer.stop()
        muxer.release()
        videoExtractor.release()
        audioExtractor.release()
    }

    private suspend fun executeYtdlpDirectDownload(
        item: QueueItem,
        notificationId: Int,
        onProgress: (progress: Float, speedBps: Long, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheDir = getApplication<Application>().cacheDir
        val tempDir = java.io.File(cacheDir, "ytdlp_job_${item.id.hashCode()}_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val cookiesPath = if (useCookies.value) prefs.getString("yt_dlp_cookies_path", null) else null
            val webViewUa = if (useCookies.value) android.webkit.WebSettings.getDefaultUserAgent(getApplication()) else null
            val customHeadersJson = getCustomHeadersJson()
            val jobContext = kotlin.coroutines.coroutineContext

            logToConsole("[yt-dlp] Executing native download pipeline...")
            val dlRes = extractor.downloadMediaYtdlp(
                url = item.url,
                outputDir = tempDir.absolutePath,
                quality = item.quality,
                mode = item.mode,
                cookiesPath = cookiesPath,
                userAgent = webViewUa,
                customHeadersJson = customHeadersJson,
                callback = object : com.material.downloader.api.PythonExtractor.YtdlpProgressCallback {
                    override fun onProgress(progress: Float, speedBps: Long, downloadedBytes: Long, totalBytes: Long) {
                        onProgress(progress, speedBps, downloadedBytes, totalBytes)
                        notificationHelper.showProgressNotification(notificationId, item.title, (progress * 100).toInt())
                    }

                    override fun isCancelled(): Boolean {
                        return jobContext[kotlinx.coroutines.Job]?.isActive == false
                    }
                }
            )

            if (dlRes.status != "success") {
                return@withContext Result.failure(Exception(dlRes.message ?: "yt-dlp download failed"))
            }

            val title = dlRes.title?.takeIf { it.isNotBlank() } ?: item.title
            val author = dlRes.author?.takeIf { it.isNotBlank() } ?: item.author
            val ext = dlRes.ext?.takeIf { it.isNotBlank() } ?: (if (item.mode == "audio") "m4a" else "mp4")
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifEmpty { "gabi_media" }.take(64)
            val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$ext"
            val targetPath = when {
                item.mode == "audio" && downloadPath.value.startsWith("Download") -> "Music/Gabi"
                else -> downloadPath.value
            }

            val finalSourceFile: java.io.File = if (!dlRes.video_path.isNullOrBlank() && !dlRes.audio_path.isNullOrBlank()) {
                val vidFile = java.io.File(dlRes.video_path)
                val audFile = java.io.File(dlRes.audio_path)
                val muxedFile = java.io.File(tempDir, "muxed.$ext")
                notificationHelper.showProgressNotification(notificationId, "$title (Muxing...)", 99)
                onProgress(0.99f, 0L, 0L, 0L)
                muxVideoAudio(vidFile, audFile, muxedFile)
                muxedFile
            } else if (!dlRes.file_path.isNullOrBlank()) {
                java.io.File(dlRes.file_path)
            } else {
                return@withContext Result.failure(Exception("No output file produced by yt-dlp"))
            }

            if (!finalSourceFile.exists() || finalSourceFile.length() == 0L) {
                return@withContext Result.failure(Exception("Downloaded file is empty"))
            }

            val targetUri = if (selectedFolderUri.value != null) {
                downloader.createSafUri(Uri.parse(selectedFolderUri.value), fileName)
            } else {
                downloader.createMediaStoreUri(fileName, targetPath)
            } ?: return@withContext Result.failure(Exception("Could not create destination file"))

            getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { outStream ->
                finalSourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream"))

            downloader.finalizeFile(targetUri, fileName)
            val cachedThumb = saveThumbnailToCache(item.thumbnail ?: dlRes.thumbnail, targetUri.toString())
            logDao.insertLog(
                DownloadLog(
                    title = title,
                    url = item.url,
                    status = "Success",
                    path = targetUri.toString(),
                    thumbnailPath = cachedThumb,
                    author = author
                )
            )
            notificationHelper.showProgressNotification(notificationId, title, 100)
            onProgress(1f, 0L, 0L, 0L)
            logToConsole("[SUCCESS] Saved via yt-dlp: $fileName")
            Result.success(targetUri.toString())
        } catch (e: Exception) {
            logToConsole("[yt-dlp] Direct download failed: ${e.message}")
            Result.failure(e)
        } finally {
            try {
                tempDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    suspend fun executeSingleQueueItem(
        item: QueueItem,
        onProgress: (progress: Float, speedBps: Long, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val notificationId = item.id.hashCode()
        val url = item.url
        val quality = item.quality
        val mode = item.mode
        val engine = item.engine

        logToConsole("Starting queue task: ${item.title}")
        onProgress(0f, 0L, 0L, 0L)

        // ytdlnis-style direct yt-dlp execution when yt-dlp is chosen explicitly or is the primary dynamic engine
        val primaryEngine = if (engine == "dynamic") getDynamicEngine(url) else engine
        if (primaryEngine == "yt-dlp") {
            val ytdlpDirect = executeYtdlpDirectDownload(item, notificationId, onProgress)
            if (ytdlpDirect.isSuccess) {
                return@withContext ytdlpDirect
            }
            if (engine == "yt-dlp") {
                val err = ytdlpDirect.exceptionOrNull()?.message ?: "yt-dlp download failed"
                notificationHelper.cancelNotification(notificationId)
                val cachedThumb = saveThumbnailToCache(item.thumbnail, null)
                logDao.insertLog(DownloadLog(title = "Failed: ${item.title}", url = url, status = "Error: $err", thumbnailPath = cachedThumb, author = item.author))
                return@withContext Result.failure(Exception(err))
            }
        }

        try {
            var result: com.material.downloader.api.ExtractionResult? = null
            val enginesToTry = if (engine == "dynamic") {
                val firstEngine = getDynamicEngine(url)
                val allEngines = listOf("cobalt", "yt-dlp", "gallery-dl", "newpipe")
                listOf(firstEngine) + allEngines.filter { it != firstEngine }
            } else {
                listOf(engine)
            }

            for (currentEngine in enginesToTry) {
                logToConsole("Queue trying engine: $currentEngine")
                val currentResult = performExtraction(url, quality, mode, currentEngine)
                if (currentResult.status == "success") {
                    result = currentResult
                    break
                } else {
                    result = currentResult
                }
            }

            if (result == null || result.status != "success") {
                val err = result?.message ?: "Extraction failed"
                val cachedThumb = saveThumbnailToCache(item.thumbnail, null)
                logDao.insertLog(DownloadLog(title = "Failed: ${item.title}", url = url, status = "Error: $err", thumbnailPath = cachedThumb, author = item.author))
                return@withContext Result.failure(Exception(err))
            }

            val customHeaders = getCustomHeadersMap()
            val mergedHeaders = (result.http_headers ?: emptyMap()) + customHeaders

            val isGallery = result.is_gallery == true
            val urlsToDownload = result.urls ?: listOf(result.url ?: return@withContext Result.failure(Exception("No download URL found")))
            val totalFiles = urlsToDownload.size

            if (isGallery && totalFiles > 1) {
                var succeededCount = 0
                var firstSavedUri: String? = null
                for ((index, downloadUrl) in urlsToDownload.withIndex()) {
                    val extension = result.ext ?: "jpg"
                    val title = result.title ?: item.title
                    val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
                    val fileName = "${sanitizedTitle}_${index + 1}_${System.currentTimeMillis()}.$extension"
                    val targetPath = when {
                        isGallery && downloadPath.value == "Downloads/Gabi" -> "Pictures/Gabi"
                        mode == "audio" && downloadPath.value == "Downloads/Gabi" -> "Music/Gabi"
                        else -> downloadPath.value
                    }

                    downloader.downloadFile(
                        url = downloadUrl,
                        fileName = fileName,
                        relativePath = targetPath,
                        customFolderUri = selectedFolderUri.value,
                        httpHeaders = mergedHeaders
                    ).collect { state ->
                        if (state is DownloadState.Downloading) {
                            val fileProgress = state.progress
                            val overallProgress = (index + fileProgress) / totalFiles
                            onProgress(overallProgress, state.speedBps, state.downloadedBytes, state.totalBytes)
                            notificationHelper.showProgressNotification(notificationId, "$title (${index + 1}/$totalFiles)", (overallProgress * 100).toInt())
                        }
                        if (state is DownloadState.Success) {
                            succeededCount++
                            val savedUri = Uri.parse(state.path)
                            downloader.finalizeFile(savedUri, fileName)
                            if (firstSavedUri == null) firstSavedUri = state.path
                        }
                    }
                }

                if (succeededCount > 0) {
                    val cachedThumb = saveThumbnailToCache(item.thumbnail ?: result.thumbnail, null)
                    logDao.insertLog(DownloadLog(title = "${result.title ?: item.title} ($succeededCount images)", url = url, status = "Success", thumbnailPath = cachedThumb, author = result.author ?: item.author))
                    notificationHelper.showProgressNotification(notificationId, result.title ?: item.title, 100)
                    onProgress(1f, 0L, 0L, 0L)
                    return@withContext Result.success(firstSavedUri ?: "")
                } else {
                    return@withContext Result.failure(Exception("Failed to download gallery images"))
                }
            } else {
                // Single stream or video/audio muxing
                val downloadUrl = urlsToDownload.first()
                val extension = result.ext ?: (if (isGallery) "jpg" else (if (mode == "audio") "mp3" else "mp4"))
                val title = result.title ?: item.title
                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifEmpty { "gabi_media" }.take(64)
                val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"
                val targetPath = when {
                    isGallery && downloadPath.value.startsWith("Download") -> "Pictures/Gabi"
                    mode == "audio" && downloadPath.value.startsWith("Download") -> "Music/Gabi"
                    else -> downloadPath.value
                }
                val audioUrl = result.audio_url

                if (audioUrl != null) {
                    val cacheDir = getApplication<Application>().cacheDir
                    val tempVideoFile = java.io.File(cacheDir, "temp_vid_${notificationId}_${System.currentTimeMillis()}.$extension")
                    val audioExt = if (extension.equals("webm", ignoreCase = true)) "webm" else "m4a"
                    val tempAudioFile = java.io.File(cacheDir, "temp_aud_${notificationId}_${System.currentTimeMillis()}.$audioExt")
                    val tempMuxedFile = java.io.File(cacheDir, "temp_mux_${notificationId}_${System.currentTimeMillis()}.$extension")

                    try {
                        var videoSuccess = false
                        var videoError: String? = null
                        downloader.downloadFileToPath(downloadUrl, tempVideoFile, mergedHeaders).collect { state ->
                            if (state is DownloadState.Downloading) {
                                val p = state.progress * 0.5f
                                onProgress(p, state.speedBps, state.downloadedBytes, state.totalBytes)
                                notificationHelper.showProgressNotification(notificationId, "$title (Video)", (p * 100).toInt())
                            }
                            if (state is DownloadState.Success) videoSuccess = true
                            if (state is DownloadState.Error) videoError = state.message
                        }
                        if (!videoSuccess) throw Exception(videoError ?: "Failed to download video stream")

                        var audioSuccess = false
                        var audioError: String? = null
                        downloader.downloadFileToPath(audioUrl, tempAudioFile, mergedHeaders).collect { state ->
                            if (state is DownloadState.Downloading) {
                                val p = 0.5f + (state.progress * 0.5f)
                                onProgress(p, state.speedBps, state.downloadedBytes, state.totalBytes)
                                notificationHelper.showProgressNotification(notificationId, "$title (Audio)", (p * 100).toInt())
                            }
                            if (state is DownloadState.Success) audioSuccess = true
                            if (state is DownloadState.Error) audioError = state.message
                        }
                        if (!audioSuccess) throw Exception(audioError ?: "Failed to download audio stream")

                        notificationHelper.showProgressNotification(notificationId, "$title (Muxing...)", 99)
                        onProgress(0.99f, 0L, 0L, 0L)
                        muxVideoAudio(tempVideoFile, tempAudioFile, tempMuxedFile)

                        val targetUri = if (selectedFolderUri.value != null) {
                            downloader.createSafUri(Uri.parse(selectedFolderUri.value), fileName)
                        } else {
                            downloader.createMediaStoreUri(fileName, targetPath)
                        } ?: throw Exception("Could not create destination file")

                        getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { outStream ->
                            tempMuxedFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }

                        downloader.finalizeFile(targetUri, fileName)
                        val cachedThumb = saveThumbnailToCache(item.thumbnail ?: result.thumbnail, targetUri.toString())
                        logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = targetUri.toString(), thumbnailPath = cachedThumb, author = result.author ?: item.author))
                        notificationHelper.showProgressNotification(notificationId, title, 100)
                        onProgress(1f, 0L, 0L, 0L)
                        return@withContext Result.success(targetUri.toString())
                    } finally {
                        if (tempVideoFile.exists()) tempVideoFile.delete()
                        if (tempAudioFile.exists()) tempAudioFile.delete()
                        if (tempMuxedFile.exists()) tempMuxedFile.delete()
                    }
                } else if (downloadUrl.contains(".m3u8", ignoreCase = true)) {
                    // HLS m3u8 stream: use yt-dlp's native Python HLS downloader
                    val cacheDir = getApplication<Application>().cacheDir
                    val tempHlsFile = java.io.File(cacheDir, "temp_hls_${notificationId}_${System.currentTimeMillis()}.$extension")
                    try {
                        notificationHelper.showProgressNotification(notificationId, "$title (HLS)", 10)
                        onProgress(0.1f, 0L, 0L, 0L)
                        val cookiesPath = if (useCookies.value) prefs.getString("yt_dlp_cookies_path", null) else null
                        val dlRes = extractor.downloadVideo(downloadUrl, tempHlsFile.absolutePath, cookiesPath, getCustomHeadersJson())
                        if (dlRes["status"] != "success" || !tempHlsFile.exists() || tempHlsFile.length() == 0L) {
                            throw Exception(dlRes["message"] ?: "HLS download failed")
                        }

                        val targetUri = if (selectedFolderUri.value != null) {
                            downloader.createSafUri(Uri.parse(selectedFolderUri.value), fileName)
                        } else {
                            downloader.createMediaStoreUri(fileName, targetPath)
                        } ?: throw Exception("Could not create destination file")

                        getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { outStream ->
                            tempHlsFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }

                        downloader.finalizeFile(targetUri, fileName)
                        val cachedThumb = saveThumbnailToCache(item.thumbnail ?: result.thumbnail, targetUri.toString())
                        logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = targetUri.toString(), thumbnailPath = cachedThumb, author = result.author ?: item.author))
                        notificationHelper.showProgressNotification(notificationId, title, 100)
                        onProgress(1f, 0L, 0L, 0L)
                        return@withContext Result.success(targetUri.toString())
                    } finally {
                        if (tempHlsFile.exists()) tempHlsFile.delete()
                    }
                } else {
                    var savedUriStr: String? = null
                    downloader.downloadFile(
                        url = downloadUrl,
                        fileName = fileName,
                        relativePath = targetPath,
                        customFolderUri = selectedFolderUri.value,
                        httpHeaders = mergedHeaders
                    ).collect { state ->
                        if (state is DownloadState.Downloading) {
                            onProgress(state.progress, state.speedBps, state.downloadedBytes, state.totalBytes)
                            notificationHelper.showProgressNotification(notificationId, title, (state.progress * 100).toInt())
                        }
                        if (state is DownloadState.Success) {
                            savedUriStr = state.path
                            val savedUri = Uri.parse(state.path)
                            downloader.finalizeFile(savedUri, fileName)
                            val cachedThumb = saveThumbnailToCache(item.thumbnail ?: result.thumbnail, state.path)
                            logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = state.path, thumbnailPath = cachedThumb, author = result.author ?: item.author))
                            notificationHelper.showProgressNotification(notificationId, title, 100)
                            onProgress(1f, 0L, 0L, 0L)
                        }
                        if (state is DownloadState.Error) {
                            throw Exception(state.message)
                        }
                    }
                    return@withContext Result.success(savedUriStr ?: "")
                }
            }
        } catch (e: Exception) {
            // If primary non-yt-dlp engine failed during download/muxing, fall back to ytdlnis-style native yt-dlp download
            if (engine != "gallery-dl") {
                logToConsole("Primary download failed (${e.message}), falling back to native yt-dlp...")
                val fallbackRes = executeYtdlpDirectDownload(item, notificationId, onProgress)
                if (fallbackRes.isSuccess) {
                    return@withContext fallbackRes
                }
            }
            val errorMsg = e.message ?: "Download failed"
            notificationHelper.cancelNotification(notificationId)
            val cachedThumb = saveThumbnailToCache(item.thumbnail, null)
            logDao.insertLog(DownloadLog(title = "Failed: ${item.title}", url = url, status = "Error: $errorMsg", thumbnailPath = cachedThumb, author = item.author))
            logToConsole("Queue task failed: $errorMsg")
            return@withContext Result.failure(e)
        }
    }

    fun downloadMedia(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        if (isPlaylistUrl(url)) {
            logToConsole("Fetching playlist tracks before download...")
            fetchPreview(url, quality, mode, engine)
            return
        }
        val title = _previewMetadata.value?.title ?: "Download"
        val thumb = _previewMetadata.value?.thumbnail
        val item = QueueItem(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            author = _previewMetadata.value?.author,
            thumbnail = thumb,
            quality = quality,
            mode = mode,
            engine = engine
        )
        queueManager.enqueue(item)
        logToConsole("Added to download queue: $title")
    }

    fun openSavedFolder() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath), "*/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            logToConsole("Could not open downloads folder: ${e.message}")
        }
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _uiState.value = DownloadState.Idle

        lastNotificationId?.let { notificationHelper.cancelNotification(it) }

        synchronized(activeTempFiles) {
            activeTempFiles.forEach { file ->
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
            }
            activeTempFiles.clear()
        }

        val resolver = getApplication<Application>().contentResolver
        synchronized(activeDownloadUris) {
            activeDownloadUris.forEach { uri ->
                try {
                    resolver.delete(uri, null, null)
                } catch (e: Exception) {
                    logToConsole("URI deletion notice: ${e.message}")
                }
            }
            activeDownloadUris.clear()
        }

        logToConsole("Download cancelled. Leftover files removed successfully.")
    }

    fun deleteLog(log: DownloadLog) {
        viewModelScope.launch { 
            log.thumbnailPath?.let { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists() && file.path.contains("thumbnails")) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            logDao.deleteLog(log) 
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch { 
            try {
                val context = getApplication<Application>()
                val thumbDir = java.io.File(context.cacheDir, "thumbnails")
                if (thumbDir.exists()) thumbDir.deleteRecursively()
            } catch (_: Exception) {}
            logDao.deleteAllLogs() 
        }
    }

    fun resetState() {
        _uiState.value = DownloadState.Idle
    }
}
