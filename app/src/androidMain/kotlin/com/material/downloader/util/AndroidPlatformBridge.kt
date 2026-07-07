package com.material.downloader.util

import android.content.Context
import android.widget.Toast
import com.material.downloader.api.PythonExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlatformBridge(private val context: Context) : PlatformBridge {
    
    private val pythonExtractor = PythonExtractor(context)
    private val fileDownloader = FileDownloader(context)

    override fun showToast(message: String) {
        // Need to run on UI thread, but standard Toast usually handles it or we can dispatch
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override suspend fun extractMediaInfo(url: String): String {
        return withContext(Dispatchers.IO) {
            pythonExtractor.extractMediaInfo(url)
        }
    }

    override suspend fun downloadMedia(
        url: String,
        quality: String,
        isAudioOnly: Boolean,
        onProgress: (Float) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Call the existing Android FileDownloader
                // Assuming it has a download method or we adapt it
                fileDownloader.downloadFile(
                    url = url,
                    quality = quality,
                    isAudioOnly = isAudioOnly,
                    onProgress = onProgress,
                    onComplete = onComplete,
                    onError = onError
                )
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
