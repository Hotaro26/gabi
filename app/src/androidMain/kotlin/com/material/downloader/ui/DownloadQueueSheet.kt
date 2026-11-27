package com.material.downloader.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.material.downloader.model.QueueItem
import com.material.downloader.model.QueueItemStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueSheet(
    queueItems: List<QueueItem>,
    maxConcurrency: Int,
    onConcurrencyChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onClearCompleted: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()
    val animatedProgress = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = true) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                animatedProgress.snapTo(backEvent.progress)
            }
            animatedProgress.animateTo(1f, tween(durationMillis = 150))
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
            }
        } catch (e: CancellationException) {
            animatedProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        } finally {
            animatedProgress.snapTo(0f)
        }
    }

    val orderedQueueItems = remember(queueItems) {
        queueItems.sortedBy { item ->
            when (item.status) {
                QueueItemStatus.DOWNLOADING -> 0
                QueueItemStatus.QUEUED -> 1
                QueueItemStatus.PAUSED -> 2
                QueueItemStatus.ERROR -> 3
                QueueItemStatus.COMPLETED -> 4
            }
        }
    }

    val p = animatedProgress.value
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .graphicsLayer {
                    scaleX = 1f - (p * 0.08f)
                    scaleY = 1f - (p * 0.08f)
                    alpha = 1f - (p * 0.3f)
                    translationY = p * 120f
                }
                .padding(horizontal = 20.dp)
        ) {
            // Header: Clean title without numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (queueItems.any { it.status == QueueItemStatus.COMPLETED || it.status == QueueItemStatus.ERROR }) {
                        IconButton(onClick = onClearCompleted) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Finished")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Queue List or Empty State
            if (orderedQueueItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Downloads in Queue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Downloads will appear here in order",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orderedQueueItems, key = { it.id }) { item ->
                        QueueItemCard(
                            item = item,
                            onPause = { onPause(item.id) },
                            onResume = { onResume(item.id) },
                            onCancel = { onCancel(item.id) },
                            onRetry = { onRetry(item.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QueueItemCard(
    item: QueueItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.progress.coerceIn(0f, 1f),
        label = "queue_progress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when (item.status) {
            QueueItemStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            QueueItemStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            QueueItemStatus.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        },
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                if (!item.thumbnail.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.mode == "audio") Icons.Rounded.Audiotrack else Icons.Rounded.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = when (item.status) {
                                    QueueItemStatus.DOWNLOADING -> "Downloading"
                                    QueueItemStatus.QUEUED -> "Queued"
                                    QueueItemStatus.PAUSED -> "Paused"
                                    QueueItemStatus.COMPLETED -> "Done"
                                    QueueItemStatus.ERROR -> "Failed"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (item.status) {
                                    QueueItemStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                                    QueueItemStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                    QueueItemStatus.ERROR -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (!item.quality.isNullOrEmpty()) {
                            Text(
                                text = "${item.quality}p",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item.status) {
                        QueueItemStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        QueueItemStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        QueueItemStatus.QUEUED -> {
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        QueueItemStatus.ERROR -> {
                            IconButton(onClick = onRetry) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss")
                            }
                        }
                        QueueItemStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Progress bar and speed metrics
            if (item.status == QueueItemStatus.DOWNLOADING || item.status == QueueItemStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val pct = (animatedProgress * 100).toInt()
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.speedBps > 0) {
                        val speedMb = item.speedBps / (1024f * 1024f)
                        Text(
                            text = String.format("%.2f MB/s", speedMb),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (item.status == QueueItemStatus.ERROR && !item.errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
