package com.material.downloader.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.material.downloader.R

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "download_channel"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(id: Int, title: String, progress: Int, folderUri: Uri? = null) {
        val intent = if (folderUri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "resource/folder")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        } else null

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val iconRes = if (progress < 100) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (progress < 100) {
            builder.setContentText("Downloading... $progress%")
                .setOngoing(true)
                .setProgress(100, progress, false)
            notificationManager.notify(id, builder.build())
        } else {
            builder.setContentText("Download Complete")
                .setOngoing(false)
                .setProgress(0, 0, false) // Remove progress bar
                .setAutoCancel(true)
            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }
            notificationManager.cancel(id) // Cancel the ongoing notification
            notificationManager.notify(id + 10000, builder.build()) // Use new ID to guarantee status bar icon refresh
        }
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
