package com.material.downloader.db

import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow

interface DownloadLogRepository {
    fun getAllLogs(): Flow<List<DownloadLog>>
    suspend fun insertLog(log: DownloadLog)
    suspend fun deleteLog(log: DownloadLog)
    suspend fun deleteAllLogs()
    suspend fun hasUrl(url: String): Boolean
}
