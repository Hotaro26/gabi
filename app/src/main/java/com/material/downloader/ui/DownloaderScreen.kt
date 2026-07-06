@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.material.downloader.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.material.downloader.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.material.downloader.util.DownloadState
import com.material.downloader.ui.theme.AppTheme
import coil.compose.AsyncImage
import androidx.documentfile.provider.DocumentFile

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DownloaderScreen(viewModel: DownloaderViewModel = viewModel()) {
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val windowSize = calculateWindowSizeClass(activity!!)
    val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var url by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("720") }
    var downloadMode by remember { mutableStateOf("auto") }
    var engine by remember { mutableStateOf("dynamic") }
    var showClearLogsDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val preview by viewModel.previewMetadata.collectAsState()
    val history by viewModel.downloadHistory.collectAsState(initial = emptyList())
    
    var showSupportedSitesDialog by remember { mutableStateOf(false) }
    var selectedHelpTab by remember { mutableStateOf("yt-dlp") }
    val externalUrl by viewModel.externalUrl.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    // Interaction states for expressive motion
    val instantInteractionSource = remember { MutableInteractionSource() }
    val isInstantPressed by instantInteractionSource.collectIsPressedAsState()
    val instantCorner by animateDpAsState(
        targetValue = if (isInstantPressed) 8.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "instant_corner"
    )
    val instantScale by animateFloatAsState(
        targetValue = if (isInstantPressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "instant_scale"
    )

    val clearLogsInteractionSource = remember { MutableInteractionSource() }
    val isClearLogsPressed by clearLogsInteractionSource.collectIsPressedAsState()
    val clearLogsCorner by animateDpAsState(
        targetValue = if (isClearLogsPressed) 8.dp else 16.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "clear_corner"
    )
    val clearLogsScale by animateFloatAsState(
        targetValue = if (isClearLogsPressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "clear_scale"
    )

    LaunchedEffect(externalUrl) {
        externalUrl?.let { 
            url = it
            viewModel.fetchPreview(it, quality, downloadMode, engine)
            viewModel.consumeSharedUrl()
        }
    }

    LaunchedEffect(url, engine, quality, downloadMode) {
        if (url.isNotBlank() && url.startsWith("http")) {
            delay(1000) // Debounce for 1 second
            viewModel.fetchPreview(url, quality, downloadMode, engine)
        }
    }

    if (showClearLogsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsDialog = false },
            title = { Text("Clear History?") },
            text = { Text("This will permanently delete all your download logs. The actual files will remain safe.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.clearAllLogs()
                        showClearLogsDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (isExpanded) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                Spacer(Modifier.height(12.dp))
                NavigationRailItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { 
                        val scale by animateFloatAsState(
                            targetValue = if (selectedTab == 0) 1.2f else 1.0f,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "home_scale"
                        )
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, 
                            null,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        ) 
                    },
                    label = { Text("Home") }
                )
                NavigationRailItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { 
                        val rotation by animateFloatAsState(
                            targetValue = if (selectedTab == 1) 360f else 0f,
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "rotation"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (selectedTab == 1) 1.2f else 1.0f,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "scale"
                        )
                        Icon(
                            Icons.Default.History, 
                            null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotation
                                scaleX = scale
                                scaleY = scale
                            }
                        ) 
                    },
                    label = { Text("Logs") }
                )
                NavigationRailItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        val rotation by animateFloatAsState(
                            targetValue = if (selectedTab == 2) 360f else 0f,
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "rotation"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (selectedTab == 2) 1.2f else 1.0f,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "scale"
                        )
                        Icon(
                            Icons.Default.Settings, 
                            null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotation
                                scaleX = scale
                                scaleY = scale
                            }
                        ) 
                    },
                    label = { Text("Settings") }
                )
            }
        }

        Scaffold(
            floatingActionButton = {
                if (selectedTab == 0 && uiState !is DownloadState.Downloading) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { showSupportedSitesDialog = true },
                            shape = RoundedCornerShape(50),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Supported Sites", modifier = Modifier.size(22.dp))
                        }
                        
                        ExtendedFloatingActionButton(
                            onClick = {
                                clipboardManager.getText()?.let { 
                                    val text = it.text
                                    url = text
                                    viewModel.fetchPreview(text, quality, downloadMode, engine)
                                    viewModel.downloadMedia(text, quality, downloadMode, engine)
                                }
                            },
                            modifier = Modifier.graphicsLayer {
                                scaleX = instantScale
                                scaleY = instantScale
                            },
                            interactionSource = instantInteractionSource,
                            shape = RoundedCornerShape(instantCorner),
                            icon = { Icon(Icons.Default.Bolt, null) },
                            text = { Text("Instant") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else if (selectedTab == 1 && history.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showClearLogsDialog = true },
                        modifier = Modifier.graphicsLayer {
                            scaleX = clearLogsScale
                            scaleY = clearLogsScale
                        },
                        interactionSource = clearLogsInteractionSource,
                        shape = RoundedCornerShape(clearLogsCorner),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Logs")
                    }
                }
            },
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { 
                                val scale by animateFloatAsState(
                                    targetValue = if (selectedTab == 0) 1.2f else 1.0f,
                                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                    label = "home_scale"
                                )
                                Icon(
                                    if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home",
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                ) 
                            },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { 
                                val rotation by animateFloatAsState(
                                    targetValue = if (selectedTab == 1) 360f else 0f,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                    label = "rotation"
                                )
                                val scale by animateFloatAsState(
                                    targetValue = if (selectedTab == 1) 1.2f else 1.0f,
                                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                    label = "scale"
                                )
                                Icon(
                                    Icons.Default.History, 
                                    null, 
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = rotation
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                ) 
                            },
                            label = { Text("Logs") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { 
                                val rotation by animateFloatAsState(
                                    targetValue = if (selectedTab == 2) 360f else 0f,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                    label = "rotation"
                                )
                                val scale by animateFloatAsState(
                                    targetValue = if (selectedTab == 2) 1.2f else 1.0f,
                                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                    label = "scale"
                                )
                                Icon(
                                    Icons.Default.Settings, 
                                    null, 
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = rotation
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                ) 
                            },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { padding ->
            Crossfade(
                targetState = selectedTab,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "tab_switch"
            ) { tab ->
                when (tab) {
                    0 -> MainDownloaderTab(
                        viewModel = viewModel,
                        uiState = uiState,
                        preview = preview,
                        url = url,
                        onUrlChange = { url = it },
                        quality = quality,
                        onQualityChange = { quality = it },
                        downloadMode = downloadMode,
                        onModeChange = { downloadMode = it },
                        engine = engine,
                        onEngineChange = { engine = it },
                        contentPadding = padding
                    )
                    1 -> LogsTab(history, viewModel::deleteLog, contentPadding = padding)
                    2 -> SettingsTab(viewModel, contentPadding = padding)
                }
            }
        }

        if (showSupportedSitesDialog) {
            AlertDialog(
                onDismissRequest = { showSupportedSitesDialog = false },
                title = { Text("Supported Sites", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("yt-dlp" to "yt-dlp", "gallery-dl" to "gallery-dl", "cobalt" to "Cobalt").forEach { (id, label) ->
                                FilterChip(
                                    selected = selectedHelpTab == id,
                                    onClick = { selectedHelpTab = id },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        
                        val sites = when (selectedHelpTab) {
                            "gallery-dl" -> listOf(
                                "Pixiv (Artwork, Manga)",
                                "Twitter/X (Images, Gifs)",
                                "Instagram (Photos, Carousels)",
                                "Reddit (Image posts)",
                                "Pinterest (Pins, Boards)",
                                "DeviantArt (Illustrations)",
                                "ArtStation (Portfolios)",
                                "Tumblr (Photo posts)",
                                "Flickr (Photos, Albums)"
                            )
                            "cobalt" -> listOf(
                                "YouTube (Videos, Shorts, Music)",
                                "TikTok (Videos, Photos)",
                                "Instagram (Reels, Stories, Posts)",
                                "Twitter/X (Videos, Gifs)",
                                "Reddit (Videos, Gifs)",
                                "Facebook (Videos, Reels)",
                                "Vimeo (Videos)",
                                "SoundCloud (Audio tracks)",
                                "Bilibili (Videos)",
                                "Dailymotion (Videos)",
                                "VK (Videos)"
                            )
                            else -> listOf(
                                "YouTube (Videos, Audio, Playlists)",
                                "TikTok (Videos, Audio, Slideshows)",
                                "Instagram (Reels, Stories, TV)",
                                "Twitter/X (Videos, Gifs)",
                                "Reddit (Videos, Audio)",
                                "Facebook (Videos, Reels)",
                                "Twitch (Clips, VODs)",
                                "SoundCloud (Tracks, Playlists)",
                                "Vimeo (Videos)",
                                "1000+ other websites..."
                            )
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sites) { site ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = site,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSupportedSitesDialog = false }) {
                        Text("Close")
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDownloaderTab(
    viewModel: DownloaderViewModel, 
    uiState: DownloadState, 
    preview: com.material.downloader.api.ExtractionResult?,
    url: String,
    onUrlChange: (String) -> Unit,
    quality: String,
    onQualityChange: (String) -> Unit,
    downloadMode: String,
    onModeChange: (String) -> Unit,
    engine: String,
    onEngineChange: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var qualityExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    var engineExpanded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val consoleLogs by viewModel.consoleLogs.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is DownloadState.Success) {
            showSuccessDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Gabi", 
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = url,
            onValueChange = { 
                onUrlChange(it)
                if (it.isEmpty()) viewModel.clearPreview() 
            },
            placeholder = { Text("Search or paste link") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Movie, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        clipboardManager.getText()?.let { 
                            onUrlChange(it.text)
                            viewModel.fetchPreview(it.text, quality, downloadMode, engine)
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(20.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        // Metadata Preview Box
        AnimatedVisibility(visible = preview != null) {
            preview?.let { meta ->
                val isImageOrGallery = meta.is_gallery == true || 
                                       meta.ext in listOf("jpg", "jpeg", "png", "webp", "gif") || 
                                       engine == "gallery-dl"
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (isImageOrGallery) {
                        Column {
                            val imageUrls = meta.urls ?: listOf(meta.thumbnail ?: meta.url ?: "")
                            val validImageUrls = imageUrls.filter { !it.isNullOrBlank() }
                            
                            if (validImageUrls.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                ) {
                                    val pagerState = rememberPagerState(pageCount = { validImageUrls.size })
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        AsyncImage(
                                            model = validImageUrls[page],
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    
                                    if (validImageUrls.size > 1) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            Text(
                                                text = "${pagerState.currentPage + 1} / ${validImageUrls.size}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(meta.title ?: "Gallery Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(meta.author ?: "Unknown Author", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (meta.urls != null && meta.urls.size > 1) {
                                        Text("${meta.urls.size} images", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp, 70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (!meta.thumbnail.isNullOrBlank()) {
                                    AsyncImage(
                                        model = meta.thumbnail,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (meta.ext == "mp3") Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meta.title ?: "Unknown Title", style = MaterialTheme.typography.titleSmall, maxLines = 2, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(meta.author ?: "Unknown Author", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (meta.size != null && meta.size > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("${"%.1f".format(meta.size / 1024f / 1024f)} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Consolidated Engine/Quality Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Engine Selector
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { engineExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val engineIcon = when (engine) {
                                "dynamic" -> Icons.Default.Bolt
                                "yt-dlp" -> Icons.Default.Movie
                                "gallery-dl" -> Icons.Default.Image
                                else -> Icons.Default.CloudDownload
                            }
                            val engineLabel = when (engine) {
                                "dynamic" -> "Dynamic (Auto)"
                                "yt-dlp" -> "yt-dlp"
                                "gallery-dl" -> "gallery-dl"
                                else -> "Cobalt"
                            }
                            Icon(engineIcon, null, modifier = Modifier.size(16.dp))
                            Text(engineLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    DropdownMenu(expanded = engineExpanded, onDismissRequest = { engineExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Dynamic (Auto)") },
                            leadingIcon = { Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp)) },
                            onClick = { 
                                onEngineChange("dynamic")
                                engineExpanded = false
                                viewModel.clearPreview()
                                viewModel.logToConsole("Switched engine to: Dynamic")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("yt-dlp") },
                            leadingIcon = { Icon(Icons.Default.Movie, null, modifier = Modifier.size(18.dp)) },
                            onClick = { 
                                onEngineChange("yt-dlp")
                                engineExpanded = false
                                viewModel.clearPreview()
                                viewModel.logToConsole("Switched engine to: yt-dlp")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("gallery-dl") },
                            leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) },
                            onClick = { 
                                onEngineChange("gallery-dl")
                                engineExpanded = false
                                viewModel.clearPreview()
                                viewModel.logToConsole("Switched engine to: gallery-dl")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cobalt") },
                            leadingIcon = { Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp)) },
                            onClick = { 
                                onEngineChange("cobalt")
                                engineExpanded = false
                                viewModel.clearPreview()
                                viewModel.logToConsole("Switched engine to: Cobalt")
                            }
                        )
                    }
                }

                Divider(modifier = Modifier.fillMaxHeight().width(1.dp).padding(vertical = 12.dp))

                // Mode/Quality Selector
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { if (engine == "yt-dlp" || engine == "cobalt") modeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = engine == "yt-dlp" || engine == "cobalt"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                            Text(if (engine == "yt-dlp" || engine == "cobalt") downloadMode.replaceFirstChar { it.uppercase() } else "Original", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        listOf("auto", "video", "audio").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                onClick = { onModeChange(mode); modeExpanded = false }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxHeight().width(1.dp).padding(vertical = 12.dp))

                // Quality Selector
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { if (engine == "yt-dlp" || engine == "cobalt") qualityExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = engine == "yt-dlp" || engine == "cobalt"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val label = if (downloadMode == "audio") {
                                when(quality) {
                                    "max" -> "Best"
                                    "1080" -> "High"
                                    "720" -> "Med"
                                    else -> "Low"
                                }
                            } else {
                                if (quality == "max") "Max" else "${quality}p"
                            }
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    DropdownMenu(expanded = qualityExpanded, onDismissRequest = { qualityExpanded = false }) {
                        val options = if (downloadMode == "audio") listOf("480" to "Low", "720" to "Medium", "1080" to "High", "max" to "Best")
                                     else listOf("480" to "480p", "720" to "720p", "1080" to "1080p", "max" to "Max Quality")
                        options.forEach { (valStr, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { onQualityChange(valStr); qualityExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        if (uiState is DownloadState.Downloading) {
            DownloadProgressBox(progress = uiState.progress, onCancel = { viewModel.cancelDownload() })
        } else {
            // Download button interaction state
            val downloadInteractionSource = remember { MutableInteractionSource() }
            val isDownloadPressed by downloadInteractionSource.collectIsPressedAsState()
            val downloadCorner by animateDpAsState(
                targetValue = if (isDownloadPressed) 6.dp else 20.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "download_corner"
            )
            val downloadScale by animateFloatAsState(
                targetValue = if (isDownloadPressed) 1.05f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "download_scale"
            )

            Button(
                onClick = { if (url.isNotBlank()) viewModel.downloadMedia(url, quality, downloadMode, engine) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = downloadScale
                        scaleY = downloadScale
                    },
                interactionSource = downloadInteractionSource,
                shape = RoundedCornerShape(downloadCorner),
                contentPadding = PaddingValues(14.dp)
            ) {
                Icon(Icons.Default.VerticalAlignBottom, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download Now")
            }
        }

        AnimatedVisibility(visible = uiState is DownloadState.Success || uiState is DownloadState.Error) {
            StatusInfo(uiState, viewModel::openSavedFolder)
        }

        Spacer(Modifier.height(16.dp))

        // Terminal CLI Component
        val currentTerminalTheme = viewModel.terminalTheme.value
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(currentTerminalTheme.background)),
            border = BorderStroke(1.dp, Color(currentTerminalTheme.header)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (macOS style window control dots)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(currentTerminalTheme.header))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                        Box(Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                        Box(Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
                    }
                    Text(
                        text = "gabi@terminal: ~",
                        color = Color(currentTerminalTheme.text).copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = { viewModel.clearConsole() },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Console",
                            tint = Color(currentTerminalTheme.text).copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                
                Divider(color = Color(currentTerminalTheme.header), thickness = 1.dp)
                
                // Logs Box
                val lazyListState = rememberLazyListState()
                LaunchedEffect(consoleLogs.size) {
                    if (consoleLogs.isNotEmpty()) {
                        lazyListState.animateScrollToItem(consoleLogs.size - 1)
                    }
                }
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(consoleLogs) { log ->
                        Text(
                            text = log,
                            color = Color(currentTerminalTheme.text),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                viewModel.resetDownloadState()
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.download_success_girl),
                        contentDescription = "Download Success Illustration",
                        modifier = Modifier
                            .size(100.dp, 100.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Finished!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Your media has been downloaded successfully.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onUrlChange("") // Reset the URL placeholder
                        viewModel.clearPreview() // Clear preview
                        viewModel.resetDownloadState() // Revert to Idle
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Download Again")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetDownloadState()
                    }
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun DownloadProgressBox(progress: Float, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Downloading...", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                strokeCap = StrokeCap.Round
            )
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.End))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(viewModel: DownloaderViewModel, contentPadding: PaddingValues = PaddingValues(0.dp)) {
    var showPlatforms by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val myUpiId = "sakibreza035@okaxis"
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.selectedFolderUri.value = it.toString()
            viewModel.selectedFolderName.value = DocumentFile.fromTreeUri(context, it)?.name ?: "Selected Folder"
        }
    }

    if (showSupportDialog) {
        // ... (rest of the code remains the same)
    }

    // ... (rest of the dialogs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
            .padding(top = 24.dp, bottom = 24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        
        // Support Section
        Text("Support Developer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Support Gabi Development", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Help keep Gabi alive and fast. Your support via UPI helps maintain the project.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val payeeName = "Hotaro"
                        val transactionNote = "Support Gabi Development"
                        val uri = Uri.parse("upi://pay").buildUpon()
                            .appendQueryParameter("pa", myUpiId)
                            .appendQueryParameter("pn", payeeName)
                            .appendQueryParameter("tn", transactionNote)
                            .appendQueryParameter("am", "0")
                            .appendQueryParameter("cu", "INR")
                            .build()
                        val upiIntent = Intent(Intent.ACTION_VIEW, uri)
                        try { context.startActivity(upiIntent) } catch (e: Exception) { showSupportDialog = true }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Payments, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Support via UPI")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Customise Section
        Text("Customise", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Appearance", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("System", "Light", "Dark").forEachIndexed { index, label ->
                        FilterChip(
                            selected = viewModel.themeMode.intValue == index,
                            onClick = { viewModel.themeMode.intValue = index },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Color Scheme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTheme.values().forEach { theme ->
                        FilterChip(
                            selected = viewModel.selectedTheme.value == theme,
                            onClick = { viewModel.selectedTheme.value = theme },
                            label = { Text(theme.label) },
                            leadingIcon = if (viewModel.selectedTheme.value == theme) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Terminal Theme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TerminalTheme.values().forEach { theme ->
                        FilterChip(
                            selected = viewModel.terminalTheme.value == theme,
                            onClick = { viewModel.updateTerminalTheme(theme) },
                            label = { Text(theme.displayName) },
                            leadingIcon = if (viewModel.terminalTheme.value == theme) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))



        // Repository Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Download Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Files will be saved in your selected folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                
                OutlinedCard(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                viewModel.selectedFolderName.value ?: "Select Folder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (viewModel.selectedFolderUri.value == null) {
                                Text("Default: Movies/Gabi", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { showPlatforms = true },
                label = { Text("Platforms") },
                leadingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = { showLicenses = true },
                label = { Text("Licenses") },
                leadingIcon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(Modifier.height(32.dp))

        // Developer Section
        Text("Developer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("hotaro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Building crisp, fast, and secure software.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hotaro26"))) },
                        label = { Text("GitHub") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    AssistChip(
                        onClick = { Toast.makeText(context, "Discord: oi.hotaro", Toast.LENGTH_LONG).show() },
                        label = { Text("Discord") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("App Info", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gabi v3.3", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Powered by yt-dlp, gallery-dl & Chaquopy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun LicenseItem(name: String, license: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun LogsTab(
    history: List<com.material.downloader.model.DownloadLog>, 
    onDelete: (com.material.downloader.model.DownloadLog) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text("Recent Downloads", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(history) { log ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                Text(log.status, style = MaterialTheme.typography.bodySmall, color = if (log.status == "Success") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { onDelete(log) }) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusInfo(state: DownloadState, onOpenFolder: () -> Unit) {
    val color = when (state) {
        is DownloadState.Success -> MaterialTheme.colorScheme.primary
        is DownloadState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (state is DownloadState.Success) Modifier.clickable { onOpenFolder() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (state is DownloadState.Error) Icons.Default.Error else Icons.Default.Movie,
                null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                val title = when (state) {
                    is DownloadState.Downloading -> "Downloading... ${(state.progress * 100).toInt()}%"
                    is DownloadState.Success -> "Done!"
                    is DownloadState.Error -> "Error"
                    else -> ""
                }
                Text(title, style = MaterialTheme.typography.labelLarge, color = color)
                if (state is DownloadState.Error) {
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1)
                } else if (state is DownloadState.Success) {
                    Text("Check your selected folder", style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}
