package com.material.downloader.db

import androidx.room.*
import com.material.downloader.model.DownloadLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadLogDao {
    @Query("SELECT * FROM download_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DownloadLog>>

    @Insert
    suspend fun insertLog(log: DownloadLog)

    @Query("DELETE FROM download_logs")
    suspend fun deleteAllLogs()

    @Delete
    suspend fun deleteLog(log: DownloadLog)
}

@Database(entities = [DownloadLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadLogDao(): DownloadLogDao
}
