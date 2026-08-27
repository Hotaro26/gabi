package com.material.downloader.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_logs")
data class DownloadLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,
    val path: String? = null,
    val thumbnailPath: String? = null,
    val author: String? = null
)
