@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.material.downloader.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var engine by remember { mutableStateOf(viewModel.getSetting("selected_engine", "newpipe")) }
    var showClearLogsDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val preview by viewModel.previewMetadata.collectAsState()
    val history by viewModel.downloadHistory.collectAsState(initial = emptyList())
    
    var showSupportedSitesDialog by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }
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
            viewModel.consumeSharedUrl()
        }
    }

    LaunchedEffect(url, engine) {
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

    var playingUrl by remember { mutableStateOf<String?>(null) }
    
    if (playingUrl != null) {
        PlayerScreen(
            url = playingUrl!!,
            onBack = { playingUrl = null },
            onDownload = { dlUrl ->
                playingUrl = null
                url = dlUrl
                engine = "newpipe"
                selectedTab = 0
            }
        )
        return
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
                        val scale by animateFloatAsState(
                            targetValue = if (selectedTab == 1) 1.2f else 1.0f,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "newpipe_scale"
                        )
                        Icon(
                            Icons.Default.PlayArrow, 
                            null,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        ) 
                    },
                    label = { Text("NewPipe") }
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
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { 
                        val rotation by animateFloatAsState(
                            targetValue = if (selectedTab == 3) 360f else 0f,
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "rotation"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (selectedTab == 3) 1.2f else 1.0f,
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
                } else if (selectedTab == 2 && history.isNotEmpty()) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bg0 by animateColorAsState(if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
                            val color0 by animateColorAsState(if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            val pad0 by animateDpAsState(if (selectedTab == 0) 24.dp else 10.dp, tween(300, easing = FastOutSlowInEasing), label = "")
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Homepage") } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).background(bg0).clickable {
                                        selectedTab = 0
                                    }.padding(horizontal = pad0, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val scale by animateFloatAsState(if (selectedTab == 0) 1.2f else 1.0f, tween(600, easing = FastOutSlowInEasing), label = "home_scale")
                                    Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, "Home", modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, tint = color0)
                                }
                            }

                            val bg1 by animateColorAsState(if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
                            val color1 by animateColorAsState(if (selectedTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            val pad1 by animateDpAsState(if (selectedTab == 1) 24.dp else 10.dp, tween(300, easing = FastOutSlowInEasing), label = "")
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Media") } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).background(bg1).clickable {
                                        selectedTab = 1
                                    }.padding(horizontal = pad1, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val scale by animateFloatAsState(if (selectedTab == 1) 1.2f else 1.0f, tween(600, easing = FastOutSlowInEasing), label = "newpipe_scale")
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, tint = color1)
                                }
                            }

                            val bg2 by animateColorAsState(if (selectedTab == 2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
                            val color2 by animateColorAsState(if (selectedTab == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            val pad2 by animateDpAsState(if (selectedTab == 2) 24.dp else 10.dp, tween(300, easing = FastOutSlowInEasing), label = "")
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("History") } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).background(bg2).clickable {
                                        selectedTab = 2
                                    }.padding(horizontal = pad2, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val rotation by animateFloatAsState(if (selectedTab == 2) 360f else 0f, tween(800, easing = FastOutSlowInEasing), label = "rotation")
                                    val scale by animateFloatAsState(if (selectedTab == 2) 1.2f else 1.0f, tween(600, easing = FastOutSlowInEasing), label = "scale")
                                    Icon(Icons.Default.History, null, modifier = Modifier.graphicsLayer { rotationZ = rotation; scaleX = scale; scaleY = scale }, tint = color2)
                                }
                            }

                            val bg3 by animateColorAsState(if (selectedTab == 3) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
                            val color3 by animateColorAsState(if (selectedTab == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            val pad3 by animateDpAsState(if (selectedTab == 3) 24.dp else 10.dp, tween(300, easing = FastOutSlowInEasing), label = "")
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Settings") } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).background(bg3).clickable {
                                        selectedTab = 3
                                    }.padding(horizontal = pad3, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val rotation by animateFloatAsState(if (selectedTab == 3) 360f else 0f, tween(800, easing = FastOutSlowInEasing), label = "rotation")
                                    val scale by animateFloatAsState(if (selectedTab == 3) 1.2f else 1.0f, tween(600, easing = FastOutSlowInEasing), label = "scale")
                                    Icon(Icons.Default.Settings, null, modifier = Modifier.graphicsLayer { rotationZ = rotation; scaleX = scale; scaleY = scale }, tint = color3)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenMargin = if (isExpanded) (configuration.screenWidthDp * 0.20f).dp else 0.dp
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = screenMargin).padding(bottom = padding.calculateBottomPadding())) {
                AnimatedContent(
                    targetState = selectedTab,
                    label = "tab_transition"
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
                        1 -> NewPipeTab(
                            viewModel = viewModel,
                            contentPadding = padding,
                            onUrlSelected = { selectedUrl ->
                                url = selectedUrl
                                engine = "newpipe"
                                selectedTab = 0
                            },
                            onWatchSelected = { selectedUrl ->
                                playingUrl = selectedUrl
                            }
                        )
                        2 -> LogsTab(history, viewModel::deleteLog, contentPadding = padding)
                        3 -> SettingsTab(viewModel, contentPadding = padding)
                    }
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
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("yt-dlp" to "yt-dlp", "gallery-dl" to "gallery-dl", "cobalt" to "Cobalt", "newpipe" to "NewPipe").forEach { (id, label) ->
                                FilterChip(
                                    selected = selectedHelpTab == id,
                                    onClick = { selectedHelpTab = id },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        
                        val sites = when (selectedHelpTab) {
                            "newpipe" -> listOf(
                                "YouTube (Videos, Audio, Shorts, Playlists)",
                                "SoundCloud (Tracks, Playlists)",
                                "Bandcamp (Tracks, Albums)",
                                "PeerTube (Videos)",
                                "media.ccc.de (Videos)"
                            )
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
                dismissButton = {
                    TextButton(onClick = { showTipsDialog = true }) {
                        Text("Help me")
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
        
        if (showTipsDialog) {
            AlertDialog(
                onDismissRequest = { showTipsDialog = false },
                title = { Text("Downloading Tips") },
                text = {
                    Text(
                        "• Use NewPipe for YouTube downloads.\n" +
                        "• Use yt-dlp as a fallback (audio extraction might take some time, so be patient).\n" +
                        "• For Pinterest and similar short-video/social sites, use Cobalt.\n" +
                        "• For images and galleries, use gallery-dl.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTipsDialog = false }) {
                        Text("Got it")
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
    var showFullscreenLogs by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    var duplicateWarningUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    BackHandler(enabled = showFullscreenLogs) {
        showFullscreenLogs = false
    }
    
    val clipboardManager = LocalClipboardManager.current
    val consoleLogs by viewModel.consoleLogs.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is DownloadState.Success) {
            showSuccessDialog = true
        }
    }

    LaunchedEffect(engine) {
        viewModel.saveSetting("selected_engine", engine)
    }

    if (duplicateWarningUrl != null) {
        AlertDialog(
            onDismissRequest = { duplicateWarningUrl = null },
            title = { Text("Duplicate Download") },
            text = { Text("You have already downloaded this media. Do you want to download it again?") },
            confirmButton = {
                TextButton(onClick = {
                    val proceedUrl = duplicateWarningUrl!!
                    duplicateWarningUrl = null
                    viewModel.downloadMedia(proceedUrl, quality, downloadMode, engine)
                }) {
                    Text("Download Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateWarningUrl = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    viewModel.updateAvailable.value?.let { updateInfo ->
        AlertDialog(
            onDismissRequest = { viewModel.updateAvailable.value = null },
            title = { Text("Update Available: ${updateInfo.first}") },
            text = { Text("A new version of Gabi is available on GitHub. Would you like to download and install it?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateAvailable.value = null
                    com.material.downloader.util.AppUpdater(context).downloadAndInstallUpdate(updateInfo.second)
                }) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateAvailable.value = null }) {
                    Text("Later")
                }
            }
        )
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
                    if (url.isNotEmpty()) {
                        IconButton(onClick = { 
                            onUrlChange("")
                            viewModel.clearPreview()
                        }) {
                            Icon(Icons.Default.Clear, "Clear URL", modifier = Modifier.size(20.dp))
                        }
                    }
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
                    onClick = { showPreviewSheet = true },
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

        if (showPreviewSheet && preview != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val meta = preview
            val isImageOrGallery = meta.is_gallery == true || meta.ext in listOf("jpg", "jpeg", "png", "webp", "gif") || engine == "gallery-dl"

            ModalBottomSheet(
                onDismissRequest = { showPreviewSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isImageOrGallery) {
                        val imageUrls = meta.urls ?: listOf(meta.thumbnail ?: meta.url ?: "")
                        val validImageUrls = imageUrls.filter { !it.isNullOrBlank() }
                        
                        if (validImageUrls.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(16.dp))
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
                                        contentScale = ContentScale.Fit
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
                    } else {
                        val streamUrlToPlay = meta.url ?: ""
                        if (streamUrlToPlay.isNotBlank()) {
                            val exoPlayer = remember { androidx.media3.exoplayer.ExoPlayer.Builder(context).build() }
                            DisposableEffect(streamUrlToPlay) {
                                exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(streamUrlToPlay)))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                                onDispose { exoPlayer.release() }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx ->
                                        androidx.media3.ui.PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    
                    Text(meta.title ?: "Unknown Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(meta.author ?: "Unknown Author", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val isAudio = downloadMode == "audio" || meta.ext == "mp3" || meta.ext == "m4a"
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(if (isAudio) Icons.Default.MusicNote else Icons.Default.PlayArrow, null)
                                Spacer(Modifier.height(4.dp))
                                Text(if (isAudio) "Audio" else "Video", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Settings, null)
                                Spacer(Modifier.height(4.dp))
                                Text(if (quality == "best") "Best Quality" else "${quality}p", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Expressive Connected Button Group (Engine/Mode/Quality)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Engine Selector
            Box(modifier = Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick = { engineExpanded = true },
                    shape = RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val engineIcon = when (engine) {
                            "newpipe" -> Icons.Default.PlayArrow
                            "yt-dlp" -> Icons.Default.Movie
                            "gallery-dl" -> Icons.Default.Image
                            else -> Icons.Default.CloudDownload
                        }
                        val engineLabel = when (engine) {
                            "newpipe" -> "NewPipe"
                            "yt-dlp" -> "yt-dlp"
                            "gallery-dl" -> "gallery-dl"
                            else -> "Cobalt"
                        }
                        Icon(engineIcon, null, modifier = Modifier.size(14.dp))
                        Text(engineLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                DropdownMenu(expanded = engineExpanded, onDismissRequest = { engineExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("NewPipe") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp)) },
                        onClick = { 
                            onEngineChange("newpipe")
                            engineExpanded = false
                            viewModel.clearPreview()
                            viewModel.logToConsole("Switched engine to: NewPipe")
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

            // Mode Selector
            Box(modifier = Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick = { if (engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe") modeExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    enabled = engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe",
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(14.dp))
                        Text(if (engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe") downloadMode.replaceFirstChar { it.uppercase() } else "Original", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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

            // Quality Selector
            Box(modifier = Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick = { if (engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe") qualityExpanded = true },
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 100.dp, bottomEnd = 100.dp),
                    enabled = engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe",
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp)
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
                        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                DropdownMenu(expanded = qualityExpanded, onDismissRequest = { qualityExpanded = false }) {
                    val options = if (downloadMode == "audio") {
                        listOf("480" to "Low", "720" to "Medium", "1080" to "High", "max" to "Best")
                    } else {
                        val avQuals = preview?.available_qualities
                        if (!avQuals.isNullOrEmpty()) {
                            avQuals.map { 
                                val clean = it.replace("p", "")
                                clean to it
                            } + listOf("max" to "Max Quality")
                        } else {
                            listOf("480" to "480p", "720" to "720p", "1080" to "1080p", "max" to "Max Quality")
                        }
                    }
                    options.forEach { (valStr, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onQualityChange(valStr); qualityExpanded = false }
                        )
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
                onClick = { 
                    if (url.isNotBlank()) {
                        coroutineScope.launch {
                            if (viewModel.hasDownloadedUrl(url)) {
                                duplicateWarningUrl = url
                            } else {
                                viewModel.downloadMedia(url, quality, downloadMode, engine)
                            }
                        }
                    } 
                },
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
            modifier = Modifier.fillMaxWidth().height(180.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { showFullscreenLogs = true }
                    )
                },
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

    if (showFullscreenLogs) {
        Dialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showFullscreenLogs = false }
        ) {
            val currentTerminalTheme = viewModel.terminalTheme.value
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(currentTerminalTheme.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(currentTerminalTheme.header))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFullscreenLogs = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(currentTerminalTheme.text)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Terminal Logs",
                            color = Color(currentTerminalTheme.text),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(consoleLogs) { log ->
                            Text(
                                text = log,
                                color = Color(currentTerminalTheme.text),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
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

@Composable
fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(viewModel: DownloaderViewModel, contentPadding: PaddingValues = PaddingValues(0.dp)) {
    var currentScreen by remember { mutableStateOf("Main") }

    BackHandler(enabled = currentScreen != "Main") {
        currentScreen = "Main"
    }
    
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState != "Main") {
                // Slide in from right, exit to left
                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it } + fadeOut()
                )
            } else {
                // Slide in from left, exit to right
                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } + fadeOut()
                )
            }.using(SizeTransform(clip = false))
        },
        label = "settings_nav"
    ) { screen ->
        when (screen) {
            "Main" -> SettingsMainList(onNavigate = { currentScreen = it }, contentPadding = contentPadding)
            "Customisation" -> CustomisationScreen(viewModel, onBack = { currentScreen = "Main" }, contentPadding = contentPadding)
            "Downloads" -> DownloadsSettingsScreen(viewModel, onBack = { currentScreen = "Main" }, contentPadding = contentPadding)
            "Developer" -> DeveloperScreen(onBack = { currentScreen = "Main" }, contentPadding = contentPadding)
            "Support" -> SupportScreen(onBack = { currentScreen = "Main" }, contentPadding = contentPadding)
        }
    }
}

@Composable
fun SettingsMainList(onNavigate: (String) -> Unit, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column {
                SettingsListItem(
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFF88637B), // Mauve
                    title = "Customisation",
                    subtitle = "Themes, App Appearance, Terminal",
                    onClick = { onNavigate("Customisation") }
                )
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                SettingsListItem(
                    icon = Icons.Default.FolderOpen,
                    iconColor = Color(0xFF00758F), // Teal
                    title = "Downloads",
                    subtitle = "Storage location, rules",
                    onClick = { onNavigate("Downloads") }
                )
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                SettingsListItem(
                    icon = Icons.Default.Code,
                    iconColor = Color(0xFF4C6B8B), // Slate blue
                    title = "Developer",
                    subtitle = "App Info, Platforms, Licenses",
                    onClick = { onNavigate("Developer") }
                )
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                SettingsListItem(
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFF7A4F5C), // Burgundy
                    title = "Support",
                    subtitle = "Help keep Gabi alive",
                    onClick = { onNavigate("Support") }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomisationScreen(viewModel: DownloaderViewModel, onBack: () -> Unit, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text("Customisation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Color Scheme", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Terminal Theme", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
    }
}

@Composable
fun DownloadsSettingsScreen(viewModel: DownloaderViewModel, onBack: () -> Unit, contentPadding: PaddingValues) {
    val context = LocalContext.current
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

    var ytCookies by remember { mutableStateOf(viewModel.getSetting("yt_dlp_cookies_path", "").isNotEmpty()) }
    var galleryCookies by remember { mutableStateOf(viewModel.getSetting("gallery_dl_cookies_path", "").isNotEmpty()) }
    var targetEngineForCookies by remember { mutableStateOf<String?>(null) }
    
    var ytDlpVersion by remember { mutableStateOf("Fetching...") }
    var galleryDlVersion by remember { mutableStateOf("Fetching...") }
    var isUpdatingExtractors by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val versions = com.material.downloader.api.PythonExtractor().getVersions()
                if (versions["status"] == "success") {
                    ytDlpVersion = versions["yt_dlp"] ?: "Unknown"
                    galleryDlVersion = versions["gallery_dl"] ?: "Unknown"
                } else {
                    ytDlpVersion = "Failed"
                    galleryDlVersion = "Failed"
                }
            } catch(e: Exception) {
                ytDlpVersion = "Error"
                galleryDlVersion = "Error"
            }
        }
    }
    
    val cookiesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val engine = targetEngineForCookies ?: return@let
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val cookiesFile = java.io.File(context.filesDir, "${engine}_cookies.txt")
                inputStream?.use { input ->
                    cookiesFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.saveSetting("${engine}_cookies_path", cookiesFile.absolutePath)
                if (engine == "yt_dlp") ytCookies = true else galleryCookies = true
                android.widget.Toast.makeText(context, "Cookies imported for $engine", android.widget.Toast.LENGTH_SHORT).show()
            } catch(e: Exception) {
                android.widget.Toast.makeText(context, "Failed to import cookies", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    var showCookieExtractor by remember { mutableStateOf(false) }

    if (showCookieExtractor && targetEngineForCookies != null) {
        CookieExtractorDialog(
            fileName = "${targetEngineForCookies}_cookies.txt",
            onDismiss = { showCookieExtractor = false },
            onCookiesExtracted = { path ->
                val engine = targetEngineForCookies!!
                viewModel.saveSetting("${engine}_cookies_path", path)
                if (engine == "yt_dlp") ytCookies = true else galleryCookies = true
                android.widget.Toast.makeText(context, "Cookies extracted for $engine", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    var showEngineChooser by remember { mutableStateOf(false) }
    var chooserAction by remember { mutableStateOf("") }
    
    if (showEngineChooser) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEngineChooser = false },
            title = { Text("Select Target Extractor") },
            text = { Text("Which extractor do you want to set cookies for?") },
            confirmButton = {
                TextButton(onClick = {
                    showEngineChooser = false
                    targetEngineForCookies = "yt_dlp"
                    if (chooserAction == "import") cookiesPickerLauncher.launch(arrayOf("text/plain"))
                    else showCookieExtractor = true
                }) { Text("yt-dlp") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEngineChooser = false
                    targetEngineForCookies = "gallery_dl"
                    if (chooserAction == "import") cookiesPickerLauncher.launch(arrayOf("text/plain"))
                    else showCookieExtractor = true
                }) { Text("gallery-dl") }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Text("Download Repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Files will be saved in your selected folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        OutlinedCard(
            onClick = { folderPickerLauncher.launch(null) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

        Spacer(Modifier.height(16.dp))
        Text("Extractors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Update internal components", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        OutlinedCard(
            onClick = {
                if (isUpdatingExtractors) return@OutlinedCard
                isUpdatingExtractors = true
                android.widget.Toast.makeText(context, "Updating extractors... Please wait", android.widget.Toast.LENGTH_LONG).show()
                val targetPath = java.io.File(context.filesDir, "python_packages").absolutePath
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val result = com.material.downloader.api.PythonExtractor().updateExtractors(targetPath)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.util.Log.e("ExtractorUpdate", "Result: $result")
                            if (result["status"] == "success") {
                                android.widget.Toast.makeText(context, "Extractors updated successfully! Please restart the app.", android.widget.Toast.LENGTH_LONG).show()
                                val versions = com.material.downloader.api.PythonExtractor().getVersions()
                                if (versions["status"] == "success") {
                                    ytDlpVersion = versions["yt_dlp"] ?: ytDlpVersion
                                    galleryDlVersion = versions["gallery_dl"] ?: galleryDlVersion
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Update failed: ${result["message"]}", android.widget.Toast.LENGTH_LONG).show()
                            }
                            isUpdatingExtractors = false
                        }
                    } catch(e: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Error updating: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            isUpdatingExtractors = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isUpdatingExtractors) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Update yt-dlp & gallery-dl", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("yt-dlp: $ytDlpVersion • gallery-dl: $galleryDlVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Extraction Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Import cookies.txt for private/age-restricted content", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        OutlinedCard(
            onClick = {
                chooserAction = "import"
                showEngineChooser = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Import cookies.txt", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (ytCookies) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("yt-dlp", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (galleryCookies) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("gallery-dl", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (ytCookies || galleryCookies) {
                    IconButton(onClick = {
                        val ytFile = java.io.File(context.filesDir, "yt_dlp_cookies.txt")
                        val galFile = java.io.File(context.filesDir, "gallery_dl_cookies.txt")
                        if (ytFile.exists()) ytFile.delete()
                        if (galFile.exists()) galFile.delete()
                        viewModel.saveSetting("yt_dlp_cookies_path", "")
                        viewModel.saveSetting("gallery_dl_cookies_path", "")
                        ytCookies = false
                        galleryCookies = false
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove cookies", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        OutlinedCard(
            onClick = {
                chooserAction = "extract"
                showEngineChooser = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Web, null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Extract from Web Login", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Login to sites to get cookies automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun saveCookiesToNetscapeFormat(context: android.content.Context, url: String, cookieString: String?, fileName: String): String {
    if (cookieString.isNullOrEmpty()) return ""
    val uri = android.net.Uri.parse(url)
    val domain = uri.host ?: return ""
    val cookiesFile = java.io.File(context.filesDir, fileName)
    val out = StringBuilder()
    if (!cookiesFile.exists()) {
        out.append("# Netscape HTTP Cookie File\n\n")
    } else {
        out.append(cookiesFile.readText())
    }
    
    val cookies = cookieString.split(";")
    for (cookie in cookies) {
        val parts = cookie.trim().split("=", limit = 2)
        if (parts.size == 2) {
            val name = parts[0]
            val value = parts[1]
            out.append("$domain\tTRUE\t/\tFALSE\t2147483647\t$name\t$value\n")
        }
    }
    cookiesFile.writeText(out.toString())
    return cookiesFile.absolutePath
}

@Composable
fun CookieExtractorDialog(fileName: String, onDismiss: () -> Unit, onCookiesExtracted: (String) -> Unit) {
    var url by remember { mutableStateOf("https://youtube.com") }
    var currentUrl by remember { mutableStateOf("https://youtube.com") }
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { currentUrl = url })
                    )
                    IconButton(onClick = { currentUrl = url }) { Icon(Icons.Default.Search, "Go") }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                        super.onPageFinished(view, loadedUrl)
                                        loadedUrl?.let { url = it }
                                    }
                                }
                                loadUrl(currentUrl)
                            }
                        },
                        update = { it.loadUrl(currentUrl) }
                    )
                }
                Button(
                    onClick = {
                        val cookieManager = CookieManager.getInstance()
                        val cookies = cookieManager.getCookie(currentUrl)
                        if (!cookies.isNullOrEmpty()) {
                            val path = saveCookiesToNetscapeFormat(context, currentUrl, cookies, fileName)
                            onCookiesExtracted(path)
                            onDismiss()
                        } else {
                            android.widget.Toast.makeText(context, "No cookies found", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("Extract & Save Cookies")
                }
            }
        }
    }
}

@Composable
fun DeveloperScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val context = LocalContext.current
    var showPlatforms by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text("Developer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://github.com/Hotaro26.png",
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text("Hotaro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Building crisp, fast, and secure software.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hotaro26"))) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(id = com.material.downloader.R.drawable.ic_github), null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GitHub")
                    }
                    OutlinedButton(
                        onClick = { Toast.makeText(context, "Discord: oi.hotaro", Toast.LENGTH_LONG).show() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(id = com.material.downloader.R.drawable.ic_discord), null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Discord")
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text("App Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Gabi v${com.material.downloader.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Powered by yt-dlp, gallery-dl & Chaquopy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val myUpiId = "9693703723@fam"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text("Support", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                        val intent = Intent(Intent.ACTION_VIEW).apply { data = uri }
                        val chooser = Intent.createChooser(intent, "Pay with...")
                        try {
                            context.startActivity(chooser)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Donate via UPI")
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        OutlinedCard(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hotaro26/gabi"))) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Column {
                    Text("Star the Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Support on GitHub", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
