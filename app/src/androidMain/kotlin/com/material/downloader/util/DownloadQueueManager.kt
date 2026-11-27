package com.material.downloader.util

import com.material.downloader.model.QueueItem
import com.material.downloader.model.QueueItemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class DownloadQueueManager(
    private val scope: CoroutineScope,
    private val downloadWorker: suspend (
        item: QueueItem,
        onProgress: (progress: Float, speedBps: Long, downloadedBytes: Long, totalBytes: Long) -> Unit
    ) -> Result<String>
) {
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private var maxConcurrency = 2
    private var semaphore = Semaphore(maxConcurrency)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun setMaxConcurrency(limit: Int) {
        val safeLimit = limit.coerceIn(1, 5)
        if (safeLimit != maxConcurrency) {
            maxConcurrency = safeLimit
            semaphore = Semaphore(safeLimit)
            triggerDispatcher()
        }
    }

    fun enqueue(item: QueueItem) {
        _queue.value = _queue.value + item
        triggerDispatcher()
    }

    fun enqueueBatch(items: List<QueueItem>) {
        _queue.value = _queue.value + items
        triggerDispatcher()
    }

    fun pause(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        updateItem(id) { it.copy(status = QueueItemStatus.PAUSED, speedBps = 0L) }
        triggerDispatcher()
    }

    fun resume(id: String) {
        updateItem(id) { it.copy(status = QueueItemStatus.QUEUED, errorMessage = null) }
        triggerDispatcher()
    }

    fun cancel(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        _queue.value = _queue.value.filter { it.id != id }
        triggerDispatcher()
    }

    fun retry(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        updateItem(id) { it.copy(status = QueueItemStatus.QUEUED, progress = 0f, errorMessage = null) }
        triggerDispatcher()
    }

    fun clearCompleted() {
        _queue.value = _queue.value.filter {
            it.status != QueueItemStatus.COMPLETED && it.status != QueueItemStatus.ERROR
        }
    }

    fun clearAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _queue.value = emptyList()
    }

    private fun updateItem(id: String, transform: (QueueItem) -> QueueItem) {
        _queue.value = _queue.value.map { if (it.id == id) transform(it) else it }
    }

    private fun triggerDispatcher() {
        val pendingItems = _queue.value.filter { it.status == QueueItemStatus.QUEUED }
        for (item in pendingItems) {
            if (activeJobs.containsKey(item.id)) continue
            startTask(item.id)
        }
    }

    private fun startTask(itemId: String) {
        val job = scope.launch(Dispatchers.IO) {
            try {
                semaphore.withPermit {
                    val current = _queue.value.find { it.id == itemId } ?: return@withPermit
                    if (current.status != QueueItemStatus.QUEUED) return@withPermit

                    updateItem(itemId) { it.copy(status = QueueItemStatus.DOWNLOADING) }

                    val result = downloadWorker(current) { progress, speedBps, downloaded, total ->
                        updateItem(itemId) {
                            it.copy(
                                progress = progress,
                                speedBps = speedBps,
                                downloadedBytes = downloaded,
                                totalBytes = total
                            )
                        }
                    }

                    if (result.isSuccess) {
                        updateItem(itemId) {
                            it.copy(
                                status = QueueItemStatus.COMPLETED,
                                progress = 1f,
                                speedBps = 0L,
                                localUri = result.getOrNull()
                            )
                        }
                    } else {
                        updateItem(itemId) {
                            it.copy(
                                status = QueueItemStatus.ERROR,
                                speedBps = 0L,
                                errorMessage = result.exceptionOrNull()?.message ?: "Download failed"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Task cancelled or paused
                } else {
                    updateItem(itemId) {
                        it.copy(
                            status = QueueItemStatus.ERROR,
                            speedBps = 0L,
                            errorMessage = e.message ?: "Task error"
                        )
                    }
                }
            } finally {
                activeJobs.remove(itemId)
                triggerDispatcher()
            }
        }

        activeJobs[itemId] = job
    }
}
