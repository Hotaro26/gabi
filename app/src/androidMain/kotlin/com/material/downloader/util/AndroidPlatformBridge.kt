package com.material.downloader.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.material.downloader.api.PythonExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlatformBridge(private val context: Context) : PlatformBridge {
    
    private val pythonExtractor = PythonExtractor(context)
    private val fileDownloader = FileDownloader(context)
    private val prefs = context.getSharedPreferences("gabi_prefs", Context.MODE_PRIVATE)

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override suspend fun extractMediaInfo(url: String): String {
        return withContext(Dispatchers.IO) {
            pythonExtractor.extractMediaInfo(url)
        }
    }

    override suspend fun downloadMedia(
        url: String, quality: String, isAudioOnly: Boolean,
        onProgress: (Float) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                fileDownloader.downloadFile(url, quality, isAudioOnly, onProgress, onComplete, onError)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    override fun openSavedFolder(path: String) {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val uri = Uri.parse("content://media/external/file/").buildUpon()
            .appendQueryParameter("path", moviesDir.absolutePath + "/" + path)
            .build()
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open folder.", Toast.LENGTH_LONG).show()
        }
    }

    override fun saveSetting(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getSetting(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }
}
