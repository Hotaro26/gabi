package com.material.downloader.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class AppUpdater(private val context: Context) {

    fun downloadAndInstallUpdate(url: String, fileName: String = "update.apk") {
        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
        
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("App Update")
            setDescription("Downloading latest version of Gabi")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(fileName)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        // Register receiver for when download is complete
        // Using context.applicationContext to avoid leaking
        context.applicationContext.registerReceiver(
            onComplete, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
    }

    fun installApk(fileName: String) {
        val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!apkFile.exists()) {
            Toast.makeText(context, "Downloaded update file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening installer", Toast.LENGTH_SHORT).show()
        }
    }
}
