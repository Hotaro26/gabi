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
import io.ktor.client.statement.*

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
    DRACULA("Dracula", 0xFF282A36, 0xFFF8F8F2, 0xFF21222C)
}

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {
    private val extractor = PythonExtractor()
    private val cobaltRepository = CobaltRepository()
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "gabi_db")
        .addMigrations(com.material.downloader.db.MIGRATION_1_2)
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
        android.util.Log.d("GabiApp", message)
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
    var isCheckingUpdates = mutableStateOf(false)
    var updateCheckMessage = mutableStateOf<String?>(null)

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        autoCheckUpdates.value = enabled
        prefs.edit().putBoolean("auto_check_updates", enabled).apply()
        logToConsole("Auto version check ${if (enabled) "enabled" else "disabled"}")
    }

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

    init {
        if (autoCheckUpdates.value) {
            checkForUpdates(manual = false)
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
                        if (latestVersion != currentVersion && !latestVersion.contains("beta", ignoreCase = true)) {
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

    private var currentDownloadJob: Job? = null
    private var lastNotificationId: Int? = null

    private val activeTempFiles = java.util.Collections.synchronizedList(mutableListOf<java.io.File>())
    private val activeDownloadUris = java.util.Collections.synchronizedList(mutableListOf<Uri>())

    private val _uiState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val uiState: StateFlow<DownloadState> = _uiState

    private val _previewMetadata = MutableStateFlow<ExtractionResult?>(null)
    val previewMetadata: StateFlow<ExtractionResult?> = _previewMetadata

    var downloadPath = mutableStateOf("Download/Gabi")
    var selectedFolderUri = mutableStateOf<String?>(null)
    var selectedFolderName = mutableStateOf<String?>(null)

    var newPipeQuery = mutableStateOf("")
    var newPipeResults = mutableStateOf<List<org.schabi.newpipe.extractor.stream.StreamInfoItem>>(emptyList())

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
                    
                    val videoOnlyStreams = urlObj.videoOnlyStreams
                    val progressiveStreams = urlObj.videoStreams
                    val qualityClean = quality.replace("p", "")

                    val selectedVideo = if (mode == "audio") {
                        null
                    } else {
                        val matchedVideoOnly = if (quality == "best" || quality.isBlank()) {
                            videoOnlyStreams.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        } else {
                            videoOnlyStreams.find { it.resolution.contains(qualityClean) }
                        }
                        
                        val matchedProgressive = if (quality == "best" || quality.isBlank()) {
                            progressiveStreams.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        } else {
                            progressiveStreams.find { it.resolution.contains(qualityClean) }
                        }
                        
                        val videoOnlyRes = matchedVideoOnly?.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        val progressiveRes = matchedProgressive?.resolution?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                        
                        if (videoOnlyRes > progressiveRes) {
                            matchedVideoOnly
                        } else {
                            matchedProgressive ?: matchedVideoOnly ?: progressiveStreams.maxByOrNull { it.resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
                        }
                    }

                    val m4aAudioStreams = urlObj.audioStreams.filter { it.getFormat()?.suffix == "m4a" }
                    val englishAudio = m4aAudioStreams.filter { it.audioLocale?.language == "en" }
                    val defaultAudio = m4aAudioStreams.filter { it.audioLocale == null }
                    
                    val bestAudio = englishAudio.maxByOrNull { it.averageBitrate }
                        ?: defaultAudio.maxByOrNull { it.averageBitrate }
                        ?: m4aAudioStreams.maxByOrNull { it.averageBitrate }
                        ?: urlObj.audioStreams.maxByOrNull { it.averageBitrate }

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
                    com.material.downloader.api.ExtractionResult(status = "error", message = e.message ?: "NewPipe extraction failed")
                }
            } else {
                logToConsole("Executing $engine extractor in Python...")
                val engineKey = if (engine == "gallery-dl") "gallery_dl" else "yt_dlp"
                val cookiesPath = if (useCookies.value) prefs.getString("${engineKey}_cookies_path", null) else null
                val res = extractor.extract(url, quality, mode, engine, cookiesPath)
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
                val enginesToTry = if (engine == "dynamic") {
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
                        logToConsole("[WARNING] Extractor $currentEngine failed. ${if (enginesToTry.last() != currentEngine) "Trying next candidate..." else "All candidates exhausted."}")
                        result = currentResult
                    }
                }

                if (result?.status == "success") {
                    logToConsole("[SUCCESS] Metadata resolved successfully!")
                    logToConsole("[INFO] Title: '${result?.title}'")
                    logToConsole("[INFO] Author: ${result?.author}")
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

    fun downloadMedia(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            _uiState.value = DownloadState.Downloading(0f)
            val notificationId = System.currentTimeMillis().toInt()
            lastNotificationId = notificationId
            logToConsole("Starting download process for $url (Engine: $engine)")
            
            try {
                logToConsole("Performing extraction for download...")
                var result: com.material.downloader.api.ExtractionResult? = null
                if (result == null) {
                    val enginesToTry = if (engine == "dynamic") {
                        val firstEngine = getDynamicEngine(url)
                        val allEngines = listOf("cobalt", "yt-dlp", "gallery-dl", "newpipe")
                        listOf(firstEngine) + allEngines.filter { it != firstEngine }
                    } else {
                        listOf(engine)
                    }

                    for (currentEngine in enginesToTry) {
                        logToConsole("Trying engine: $currentEngine")
                        val currentResult = performExtraction(url, quality, mode, currentEngine)
                        if (currentResult.status == "success") {
                            result = currentResult
                            break
                        } else {
                            logToConsole("Engine $currentEngine failed. ${if (enginesToTry.last() != currentEngine) "Trying next..." else "No more engines to try."}")
                            result = currentResult
                        }
                    }
                }

                if (result == null || result.status != "success") {
                    throw Exception(result?.message ?: "All extraction engines failed.")
                }
                
                if (result?.status == "success") {
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
                            
                            // Automatically save galleries to Pictures/Gabi and audio to Music/Gabi
                            val targetPath = when {
                                isGallery && downloadPath.value == "Downloads/Gabi" -> "Pictures/Gabi"
                                mode == "audio" && downloadPath.value == "Downloads/Gabi" -> "Music/Gabi"
                                else -> downloadPath.value
                            }
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
                                    _uiState.value = DownloadState.Downloading(overallProgress, state.downloadedBytes, state.totalBytes, state.speedBps)
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
                            val cachedThumb = saveThumbnailToCache(_previewMetadata.value?.thumbnail, null)
                            logDao.insertLog(DownloadLog(title = "${result.title ?: "Gallery"} ($succeededCount images)", url = url, status = "Success", thumbnailPath = cachedThumb))
                            notificationHelper.showProgressNotification(notificationId, result.title ?: "Gallery", 100)
                            logToConsole("Gallery download complete. Successfully saved $succeededCount/$totalFiles files.")
                        } else {
                            throw Exception("Failed to download gallery images")
                        }
                    } else {
                        // Single file download
                        val downloadUrl = urlsToDownload.first()
                        val extension = result.ext ?: (if (isGallery) "jpg" else "mp4")
                        val title = result.title ?: (if (isGallery) "image" else "video")
                        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
                        val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"
                        
                        val targetPath = when {
                            isGallery && downloadPath.value == "Download/Gabi" -> "Pictures/Gabi"
                            mode == "audio" && downloadPath.value == "Download/Gabi" -> "Music/Gabi"
                            else -> downloadPath.value
                        }

                        val audioUrl = result.audio_url
                        
                        suspend fun downloadTrack(trackUrl: String, destFile: java.io.File, trackName: String, progressOffset: Float, progressScale: Float): Boolean {
                            if (trackUrl.contains(".m3u8")) {
                                logToConsole("Downloading $trackName track with yt-dlp native downloader...")
                                val engineKey = if (engine == "gallery-dl") "gallery_dl" else "yt_dlp"
                                val cookiesPath = if (useCookies.value) prefs.getString("${engineKey}_cookies_path", null) else null
                                val pythonResult = extractor.downloadVideo(trackUrl, destFile.absolutePath, cookiesPath)
                                if (pythonResult["status"] == "success") {
                                    _uiState.value = DownloadState.Downloading(progressOffset + progressScale)
                                    return true
                                } else {
                                    logToConsole("$trackName download failed: ${pythonResult["message"]}")
                                    return false
                                }
                            } else {
                                logToConsole("Downloading $trackName track via Ktor: $trackUrl")
                                var success = false
                                downloader.downloadFileToPath(trackUrl, destFile).collect { state ->
                                    if (state is DownloadState.Downloading) {
                                        val progress = progressOffset + (state.progress * progressScale)
                                        _uiState.value = DownloadState.Downloading(progress, state.downloadedBytes, state.totalBytes, state.speedBps)
                                        notificationHelper.showProgressNotification(notificationId, "$title ($trackName)", (progress * 100).toInt())
                                    }
                                    if (state is DownloadState.Success) {
                                        success = true
                                    }
                                    if (state is DownloadState.Error) {
                                        logToConsole("$trackName download failed: ${state.message}")
                                    }
                                }
                                return success
                            }
                        }

                        if (audioUrl != null) {
                            logToConsole("Split video and audio streams detected. Starting downloads...")
                            val cacheDir = getApplication<Application>().cacheDir
                            val tempVideoFile = java.io.File(cacheDir, "temp_video_${System.currentTimeMillis()}.$extension")
                            val tempAudioFile = java.io.File(cacheDir, "temp_audio_${System.currentTimeMillis()}.m4a")
                            val tempMuxedFile = java.io.File(cacheDir, "temp_muxed_${System.currentTimeMillis()}.$extension")

                            try {
                                val videoSuccess = downloadTrack(downloadUrl, tempVideoFile, "Video", 0f, 0.5f)
                                if (!videoSuccess) throw Exception("Failed to download video track")

                                val audioSuccess = downloadTrack(audioUrl, tempAudioFile, "Audio", 0.5f, 0.5f)
                                if (!audioSuccess) throw Exception("Failed to download audio track")

                                logToConsole("Muxing video and audio tracks...")
                                _uiState.value = DownloadState.Downloading(0.99f)
                                notificationHelper.showProgressNotification(notificationId, "$title (Muxing...)", 99)
                                
                                muxVideoAudio(tempVideoFile, tempAudioFile, tempMuxedFile)
                                logToConsole("Muxing completed successfully!")

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
                                val cachedThumb = saveThumbnailToCache(_previewMetadata.value?.thumbnail, targetUri.toString())
                                logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = targetUri.toString(), thumbnailPath = cachedThumb))
                                logToConsole("File saved successfully to $targetPath/$fileName")
                                
                                _uiState.value = DownloadState.Success(targetUri.toString())

                                val folderUri = if (selectedFolderUri.value != null) {
                                    Uri.parse(selectedFolderUri.value)
                                } else {
                                    val mediaDir = when {
                                        targetPath.startsWith("Pictures") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                        targetPath.startsWith("Music") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                                        targetPath.startsWith("Downloads") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                    }
                                    Uri.parse("content://media/external/file/").buildUpon()
                                        .appendQueryParameter("path", mediaDir.absolutePath + "/" + targetPath)
                                        .build()
                                }
                                notificationHelper.showProgressNotification(notificationId, title, 100, folderUri)
                            } finally {
                                if (tempVideoFile.exists()) tempVideoFile.delete()
                                if (tempAudioFile.exists()) tempAudioFile.delete()
                                if (tempMuxedFile.exists()) tempMuxedFile.delete()
                            }
                        } else if (downloadUrl.contains(".m3u8")) {
                            logToConsole("Single HLS stream detected. Starting native download...")
                            val cacheDir = getApplication<Application>().cacheDir
                            val tempFinalFile = java.io.File(cacheDir, "temp_single_${System.currentTimeMillis()}.$extension")
                            
                            try {
                                val success = downloadTrack(downloadUrl, tempFinalFile, "Media", 0f, 1f)
                                if (!success) throw Exception("Failed to download HLS stream")
                                
                                val targetUri = if (selectedFolderUri.value != null) {
                                    downloader.createSafUri(Uri.parse(selectedFolderUri.value), fileName)
                                } else {
                                    downloader.createMediaStoreUri(fileName, targetPath)
                                } ?: throw Exception("Could not create destination file")
                                
                                getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { outStream ->
                                    tempFinalFile.inputStream().use { inStream ->
                                        inStream.copyTo(outStream)
                                    }
                                }
                                
                                downloader.finalizeFile(targetUri, fileName)
                                val cachedThumb = saveThumbnailToCache(_previewMetadata.value?.thumbnail, targetUri.toString())
                                logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = targetUri.toString(), thumbnailPath = cachedThumb))
                                logToConsole("File saved successfully to $targetPath/$fileName")
                                _uiState.value = DownloadState.Success(targetUri.toString())
                                
                                val folderUri = if (selectedFolderUri.value != null) {
                                    Uri.parse(selectedFolderUri.value)
                                } else {
                                    val mediaDir = when {
                                        targetPath.startsWith("Pictures") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                        targetPath.startsWith("Music") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                                        targetPath.startsWith("Downloads") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                                    }
                                    Uri.parse("content://media/external/file/").buildUpon()
                                        .appendQueryParameter("path", mediaDir.absolutePath + "/" + targetPath)
                                        .build()
                                }
                                notificationHelper.showProgressNotification(notificationId, title, 100, folderUri)
                            } finally {
                                if (tempFinalFile.exists()) tempFinalFile.delete()
                            }
                        } else {
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
                                    val cachedThumb = saveThumbnailToCache(_previewMetadata.value?.thumbnail, state.path)
                                    logDao.insertLog(DownloadLog(title = title, url = url, status = "Success", path = state.path, thumbnailPath = cachedThumb))
                                    logToConsole("File saved successfully to $targetPath/$fileName")
                                    
                                    val folderUri = if (selectedFolderUri.value != null) {
                                        Uri.parse(selectedFolderUri.value)
                                    } else {
                                        val mediaDir = when {
                                            targetPath.startsWith("Pictures") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                            targetPath.startsWith("Music") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                                            targetPath.startsWith("Downloads") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
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
                    }
                } else {
                    val errorMsg = result?.message ?: "Extraction failed"
                    _uiState.value = DownloadState.Error(errorMsg)
                    val cachedThumb = saveThumbnailToCache(_previewMetadata.value?.thumbnail, null)
                    logDao.insertLog(DownloadLog(title = "Failed Download", url = url, status = "Error: $errorMsg", thumbnailPath = cachedThumb))
                    logToConsole("Download failed: $errorMsg")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is kotlinx.coroutines.CancellationException) {
                    _uiState.value = DownloadState.Idle
                    lastNotificationId?.let { notificationHelper.cancelNotification(it) }
                    logToConsole("Download cancelled")
                } else {
                    val errorMsg = e.message ?: "Unknown error"
                    _uiState.value = DownloadState.Error(errorMsg)
                    val cachedThumb = saveThumbnailToCache(_previewMetadata.value?.thumbnail, null)
                    logDao.insertLog(DownloadLog(title = "Failed Download", url = url, status = "Error: $errorMsg", thumbnailPath = cachedThumb))
                    logToConsole("Download exception: $errorMsg")
                }
            }
        }
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
