package com.material.downloader.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.security.MessageDigest

object DownloadArchiveManager {
    private const val ARCHIVE_FILE_NAME = "download_archive.txt"
    private const val PREF_ARCHIVE_ENABLED = "download_archive_enabled"

    private val lock = Any()
    private val _archiveCount = MutableStateFlow(0)
    val archiveCount: StateFlow<Int> = _archiveCount.asStateFlow()

    private val _archiveEnabled = MutableStateFlow(true)
    val archiveEnabled: StateFlow<Boolean> = _archiveEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("gabi_prefs", Context.MODE_PRIVATE)
        _archiveEnabled.value = prefs.getBoolean(PREF_ARCHIVE_ENABLED, true)
        refreshCount(context)
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("gabi_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(PREF_ARCHIVE_ENABLED, true)
        _archiveEnabled.value = enabled
        return enabled
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("gabi_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_ARCHIVE_ENABLED, enabled)
            .apply()
        _archiveEnabled.value = enabled
    }

    fun getArchiveFile(context: Context): File {
        return File(context.filesDir, ARCHIVE_FILE_NAME)
    }

    private fun readLinesLocked(context: Context): LinkedHashSet<String> {
        val file = getArchiveFile(context)
        if (!file.exists()) return linkedSetOf()
        val lines = linkedSetOf<String>()
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed)
            }
        }
        return lines
    }

    private fun writeLinesLocked(context: Context, lines: Collection<String>) {
        val file = getArchiveFile(context)
        val content = if (lines.isEmpty()) "" else lines.joinToString("\n", postfix = "\n")
        file.writeText(content)
        _archiveCount.value = lines.size
    }

    fun refreshCount(context: Context) {
        synchronized(lock) {
            _archiveCount.value = readLinesLocked(context).size
        }
    }

    fun isArchived(
        context: Context,
        url: String,
        extractorKey: String? = null,
        mediaId: String? = null
    ): Boolean {
        if (!isEnabled(context)) return false
        val candidates = buildCandidateKeys(url, extractorKey, mediaId)
        synchronized(lock) {
            val existing = readLinesLocked(context)
            return candidates.any { existing.contains(it) }
        }
    }

    fun addSuccessEntry(
        context: Context,
        url: String,
        extractorKey: String? = null,
        mediaId: String? = null
    ) {
        val candidates = buildCandidateKeys(url, extractorKey, mediaId)
        if (candidates.isEmpty()) return
        synchronized(lock) {
            val existing = readLinesLocked(context)
            var changed = false
            for (entry in candidates) {
                if (existing.add(entry)) {
                    changed = true
                }
            }
            if (changed) {
                writeLinesLocked(context, existing)
            }
        }
    }

    fun readAllText(context: Context): String {
        synchronized(lock) {
            val file = getArchiveFile(context)
            return if (file.exists()) file.readText() else ""
        }
    }

    fun writeAllText(context: Context, text: String) {
        synchronized(lock) {
            val cleaned = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .distinct()
                .toList()
            writeLinesLocked(context, cleaned)
        }
    }

    fun importFromStream(context: Context, input: InputStream): Int {
        val importedLines = input.bufferedReader().use { reader ->
            reader.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }
        if (importedLines.isEmpty()) return 0
        synchronized(lock) {
            val existing = readLinesLocked(context)
            var added = 0
            for (line in importedLines) {
                if (existing.add(line)) added++
            }
            if (added > 0) {
                writeLinesLocked(context, existing)
            }
            return added
        }
    }

    fun exportToStream(context: Context, output: OutputStream) {
        val text = readAllText(context)
        output.bufferedWriter().use { writer ->
            writer.write(text)
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            writeLinesLocked(context, emptyList())
        }
    }

    fun buildCandidateKeys(
        url: String,
        extractorKey: String? = null,
        mediaId: String? = null
    ): List<String> {
        val keys = linkedSetOf<String>()
        if (!extractorKey.isNullOrBlank() && !mediaId.isNullOrBlank()) {
            val normPlatform = extractorKey.trim().lowercase()
                .removeSuffix("ie")
                .replace(Regex("[^a-z0-9._-]"), "")
            val normId = mediaId.trim()
            if (normPlatform.isNotEmpty() && normId.isNotEmpty()) {
                keys.add("$normPlatform $normId")
            }
        }
        val (urlPlatform, urlId) = extractPlatformAndIdFromUrl(url)
        if (urlPlatform.isNotEmpty() && urlId.isNotEmpty()) {
            keys.add("$urlPlatform $urlId")
        }
        return keys.toList()
    }

    fun extractPlatformAndIdFromUrl(rawUrl: String): Pair<String, String> {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return "" to ""
        return try {
            val uri = URI(trimmed)
            val host = (uri.host ?: "").lowercase().removePrefix("www.").removePrefix("m.")
            val path = uri.path ?: ""
            val query = uri.query ?: ""

            when {
                host.contains("youtube.com") || host == "youtu.be" -> {
                    val vParam = query.split("&")
                        .firstOrNull { it.startsWith("v=") }
                        ?.substringAfter("v=")
                        ?.substringBefore("&")
                    val id = when {
                        !vParam.isNullOrBlank() -> vParam
                        host == "youtu.be" -> path.trim('/').substringBefore('/')
                        path.startsWith("/shorts/") -> path.removePrefix("/shorts/").substringBefore('/')
                        path.startsWith("/live/") -> path.removePrefix("/live/").substringBefore('/')
                        path.startsWith("/embed/") -> path.removePrefix("/embed/").substringBefore('/')
                        else -> sha256Short(trimmed)
                    }
                    "youtube" to id
                }
                host.contains("pinterest.") || host == "pin.it" -> {
                    val pinMatch = Regex("/pin/([0-9A-Za-z_-]+)").find(path)?.groupValues?.get(1)
                    "pinterest" to (pinMatch ?: path.trim('/').ifEmpty { sha256Short(trimmed) })
                }
                host.contains("instagram.com") -> {
                    val igMatch = Regex("/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)").find(path)?.groupValues?.get(1)
                    "instagram" to (igMatch ?: sha256Short(trimmed))
                }
                host.contains("tiktok.com") -> {
                    val ttMatch = Regex("/video/([0-9]+)").find(path)?.groupValues?.get(1)
                    "tiktok" to (ttMatch ?: sha256Short(trimmed))
                }
                host == "twitter.com" || host == "x.com" -> {
                    val twMatch = Regex("/status/([0-9]+)").find(path)?.groupValues?.get(1)
                    "twitter" to (twMatch ?: sha256Short(trimmed))
                }
                host.contains("reddit.com") || host == "redd.it" -> {
                    val rdMatch = Regex("/comments/([A-Za-z0-9]+)").find(path)?.groupValues?.get(1)
                    "reddit" to (rdMatch ?: sha256Short(trimmed))
                }
                else -> {
                    val platform = host.ifEmpty { "generic" }.substringBefore('.')
                    platform to sha256Short(trimmed)
                }
            }
        } catch (_: Exception) {
            "generic" to sha256Short(trimmed)
        }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
