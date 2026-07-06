import re

with open("app/src/main/java/com/material/downloader/ui/DownloaderViewModel.kt", "r") as f:
    content = f.read()

perform_extraction_code = """    private suspend fun performExtraction(url: String, quality: String, mode: String, engine: String): ExtractionResult {
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
            } else {
                logToConsole("Executing $engine extractor in Python...")
                val res = extractor.extract(url, quality, mode, engine)
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

"""

# Find the end of properties / init block to insert performExtraction. We can insert it before fetchPreview
fetch_preview_regex = r"(    fun fetchPreview\()"
content = re.sub(fetch_preview_regex, perform_extraction_code + r"\1", content, count=1)

# Now update fetchPreview body
fetch_preview_new = """    fun fetchPreview(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        viewModelScope.launch {
            logToConsole("Fetching preview: $url (Engine: $engine, Mode: $mode)")
            try {
                var result: com.material.downloader.api.ExtractionResult? = null
                val enginesToTry = if (engine == "dynamic") {
                    val firstEngine = getDynamicEngine(url)
                    val allEngines = listOf("cobalt", "yt-dlp", "gallery-dl")
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

                if (result?.status == "success") {
                    logToConsole("Preview loaded: '${result?.title}' by ${result?.author}")
                }
                _previewMetadata.value = result
            } catch (e: Exception) {
                logToConsole("Preview exception: ${e.message}")
                _previewMetadata.value = com.material.downloader.api.ExtractionResult(status = "error", message = e.message ?: "Extraction failed")
            }
        }
    }"""

old_fetch_preview_regex = r"    fun fetchPreview.*?_previewMetadata\.value = ExtractionResult\(status = \"error\", message = e\.message \?: \"Extraction failed\"\)\n            }\n        }\n    }"
content = re.sub(old_fetch_preview_regex, fetch_preview_new, content, flags=re.DOTALL)


download_media_new = """    fun downloadMedia(url: String, quality: String, mode: String = "auto", engine: String = "dynamic") {
        currentDownloadJob?.cancel()
        currentDownloadJob = viewModelScope.launch {
            _uiState.value = DownloadState.Downloading(0f)
            val notificationId = System.currentTimeMillis().toInt()
            lastNotificationId = notificationId
            logToConsole("Starting download process for $url (Engine: $engine)")
            
            try {
                var result = _previewMetadata.value
                if (result == null) {
                    logToConsole("No cached preview. Performing extraction first...")
                    val enginesToTry = if (engine == "dynamic") {
                        val firstEngine = getDynamicEngine(url)
                        val allEngines = listOf("cobalt", "yt-dlp", "gallery-dl")
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
                
                if (result?.status == "success") {"""

old_download_media_regex = r"    fun downloadMedia.*?if \(result\.status == \"success\"\) \{"
content = re.sub(old_download_media_regex, download_media_new, content, flags=re.DOTALL)

with open("app/src/main/java/com/material/downloader/ui/DownloaderViewModel.kt", "w") as f:
    f.write(content)

