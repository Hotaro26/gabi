package com.material.downloader.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.material.downloader.model.QueueItem
import com.material.downloader.model.QueueItemStatus

@Composable
fun DownloadQueueScreen(
    queueItems: List<QueueItem>,
    maxConcurrency: Int,
    onConcurrencyChange: (Int) -> Unit,
    onBack: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onClearCompleted: () -> Unit
) {
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

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 }
    }
    val elevation by animateDpAsState(if (isScrolled) 4.dp else 0.dp, label = "queue_elevation")
    val surfaceColor by animateColorAsState(
        if (isScrolled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background,
        label = "queue_surface_color"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pinned Top Bar with back arrow (no numbers at top)
        Surface(
            tonalElevation = elevation,
            shadowElevation = elevation,
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (queueItems.any { it.status == QueueItemStatus.COMPLETED || it.status == QueueItemStatus.ERROR }) {
                    FilledTonalIconButton(
                        onClick = onClearCompleted,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Finished", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Parallel Downloads Control Bar
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Parallel Downloads",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 4).forEach { count ->
                            val isSelected = maxConcurrency == count
                            Surface(
                                onClick = { onConcurrencyChange(count) },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (item.status) {
                        QueueItemStatus.DOWNLOADING -> {
                            FilledTonalIconButton(
                                onClick = onPause,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(
                                onClick = onCancel,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                            }
                        }
                        QueueItemStatus.PAUSED -> {
                            FilledTonalIconButton(
                                onClick = onResume,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(
                                onClick = onCancel,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                            }
                        }
                        QueueItemStatus.QUEUED -> {
                            FilledTonalIconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                            }
                        }
                        QueueItemStatus.ERROR -> {
                            FilledTonalIconButton(
                                onClick = onRetry,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                            }
                        }
                        QueueItemStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp).size(22.dp)
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
