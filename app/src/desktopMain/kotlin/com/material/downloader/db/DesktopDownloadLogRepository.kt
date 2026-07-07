package com.material.downloader.db

import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopDownloadLogRepository : DownloadLogRepository {
    private val logs = MutableStateFlow<List<DownloadLog>>(emptyList())
    override fun getAllLogs(): Flow<List<DownloadLog>> = logs
    override suspend fun insertLog(log: DownloadLog) {
        val current = logs.value.toMutableList()
        current.add(log)
        logs.value = current
    }
    override suspend fun deleteLog(log: DownloadLog) {
        val current = logs.value.toMutableList()
        current.remove(log)
        logs.value = current
    }
    override suspend fun deleteAllLogs() {
        logs.value = emptyList()
    }
}
