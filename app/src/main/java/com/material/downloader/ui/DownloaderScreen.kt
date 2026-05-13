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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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
    var engine by remember { mutableStateOf("yt-dlp") }
    var showClearLogsDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.downloadHistory.collectAsState(initial = emptyList())
    val preview by viewModel.previewMetadata.collectAsState()
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
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Icon(Icons.Default.Movie, null, modifier = Modifier.padding(vertical = 12.dp))
                }
            ) {
                NavigationRailItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") }
                )
                NavigationRailItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Logs") }
                )
                NavigationRailItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }

        Scaffold(
            floatingActionButton = {
                if (selectedTab == 0 && uiState !is DownloadState.Downloading) {
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
                                Icon(
                                    if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
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
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "rotation"
                                )
                                Icon(Icons.Default.History, null, modifier = Modifier.rotate(rotation)) 
                            },
                            label = { Text("Logs") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { 
                                val rotation by animateFloatAsState(
                                    targetValue = if (selectedTab == 2) 360f else 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "rotation"
                                )
                                Icon(Icons.Default.Settings, null, modifier = Modifier.rotate(rotation)) 
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
    
    val clipboardManager = LocalClipboardManager.current

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = meta.thumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp, 60.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.title ?: "Unknown Title", style = MaterialTheme.typography.labelLarge, maxLines = 1, fontWeight = FontWeight.Bold)
                            Text(meta.author ?: "Unknown Author", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            if (meta.size != null && meta.size > 0) {
                                Text("${"%.1f".format(meta.size / 1024f / 1024f)} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                            Icon(if (engine == "yt-dlp") Icons.Default.Movie else Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                            Text(if (engine == "yt-dlp") "Media" else "Gallery", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    DropdownMenu(expanded = engineExpanded, onDismissRequest = { engineExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Video / Audio") },
                            leadingIcon = { Icon(Icons.Default.Movie, null, modifier = Modifier.size(18.dp)) },
                            onClick = { onEngineChange("yt-dlp"); engineExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Image Gallery") },
                            leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) },
                            onClick = { onEngineChange("gallery-dl"); engineExpanded = false }
                        )
                    }
                }

                Divider(modifier = Modifier.fillMaxHeight().width(1.dp).padding(vertical = 12.dp))

                // Mode/Quality Selector
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { if (engine == "yt-dlp") modeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = engine == "yt-dlp"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                            Text(if (engine == "yt-dlp") downloadMode.replaceFirstChar { it.uppercase() } else "Original", style = MaterialTheme.typography.bodySmall)
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
                        onClick = { if (engine == "yt-dlp") qualityExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = engine == "yt-dlp"
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
