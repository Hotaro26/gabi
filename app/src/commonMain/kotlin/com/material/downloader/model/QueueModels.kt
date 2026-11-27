package com.material.downloader.model

enum class QueueItemStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class QueueItem(
    val id: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val thumbnail: String? = null,
    val quality: String = "720",
    val mode: String = "auto",
    val engine: String = "dynamic",
    val status: QueueItemStatus = QueueItemStatus.QUEUED,
    val progress: Float = 0f,
    val speedBps: Long = 0L,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
    val localUri: String? = null,
    val playlistId: String? = null
)
