package com.material.downloader.db

import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow

class AndroidDownloadLogRepository(private val dao: DownloadLogDao) : DownloadLogRepository {
    override fun getAllLogs(): Flow<List<DownloadLog>> = dao.getAllLogs()
    override suspend fun insertLog(log: DownloadLog) = dao.insertLog(log)
    override suspend fun deleteLog(log: DownloadLog) = dao.deleteLog(log)
    override suspend fun deleteAllLogs() = dao.deleteAllLogs()
}
