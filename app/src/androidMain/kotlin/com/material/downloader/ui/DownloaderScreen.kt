@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.material.downloader.ui

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue


import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
import org.burnoutcrew.reorderable.*
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
    
    val instantInteractionSource = remember { MutableInteractionSource() }
    val isInstantPressed by instantInteractionSource.collectIsPressedAsState()
    val instantScale by animateFloatAsState(
        targetValue = if (isInstantPressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "instant_scale"
    )
    
    var showSpeechNotSupportedDialog by remember { mutableStateOf(false) }
    var isSettingsSearchActive by remember { mutableStateOf(false) }
    var settingsSearchQuery by remember { mutableStateOf("") }
    val searchInteractionSource = remember { MutableInteractionSource() }
    val isSearchPressed by searchInteractionSource.collectIsPressedAsState()
    val searchScale by animateFloatAsState(
        targetValue = if (isSearchPressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "search_scale"
    )
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                viewModel.voiceSearchEvent.tryEmit(matches[0])
            }
        }
    }

    val voiceInteractionSource = remember { MutableInteractionSource() }
    val isVoicePressed by voiceInteractionSource.collectIsPressedAsState()
    val voiceScale by animateFloatAsState(
        targetValue = if (isVoicePressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "voice_scale"
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
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.this_will_permanently_delete_a)) },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.clearAllLogs()
                        showClearLogsDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) { Text(stringResource(R.string.cancel)) }
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
                    label = { Text(stringResource(R.string.home)) }
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
                    label = { Text(stringResource(R.string.newpipe)) }
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
                    label = { Text(stringResource(R.string.logs)) }
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
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }

        val hazeState = remember { HazeState() }
        var showHelpFab by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(5000)
            showHelpFab = false
        }
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            floatingActionButton = {
                if (showHelpFab && selectedTab == 0 && uiState !is DownloadState.Downloading) {
                    FloatingActionButton(
                        onClick = { showSupportedSitesDialog = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Supported Sites", modifier = Modifier.size(22.dp))
                    }
                }
            },
            bottomBar = {
                if (!isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .let { m ->
                                    if (viewModel.isNavBarOpaque.value) {
                                        m.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    } else if (viewModel.isNavBarTrueGlass.value) {
                                        m.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = Color.White.copy(alpha = 0.05f), blurRadius = 32.dp))
                                    } else if (viewModel.isNavBarBlurEnabled.value) {
                                        m.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), blurRadius = 24.dp))
                                    } else {
                                        m.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), CircleShape)
                                    }
                                }
                                .animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bg0 by animateColorAsState(if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "")
                            val color0 by animateColorAsState(if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                            val pad0 by animateDpAsState(if (selectedTab == 0) 24.dp else 10.dp, tween(300, easing = FastOutSlowInEasing), label = "")
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text(stringResource(R.string.homepage)) } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).let { m ->
                                        if (selectedTab == 0 && viewModel.isNavBarTrueGlass.value) {
                                            m.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), blurRadius = 16.dp)).background(Color.Transparent)
                                        } else {
                                            m.background(bg0)
                                        }
                                    }.clickable {
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
                                tooltip = { PlainTooltip { Text(stringResource(R.string.media)) } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).let { m ->
                                        if (selectedTab == 1 && viewModel.isNavBarTrueGlass.value) {
                                            m.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), blurRadius = 16.dp)).background(Color.Transparent)
                                        } else {
                                            m.background(bg1)
                                        }
                                    }.clickable {
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
                                tooltip = { PlainTooltip { Text(stringResource(R.string.history)) } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).let { m ->
                                        if (selectedTab == 2 && viewModel.isNavBarTrueGlass.value) {
                                            m.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), blurRadius = 16.dp)).background(Color.Transparent)
                                        } else {
                                            m.background(bg2)
                                        }
                                    }.clickable {
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
                                tooltip = { PlainTooltip { Text(stringResource(R.string.settings)) } },
                                state = rememberTooltipState()
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).let { m ->
                                        if (selectedTab == 3 && viewModel.isNavBarTrueGlass.value) {
                                            m.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), blurRadius = 16.dp)).background(Color.Transparent)
                                        } else {
                                            m.background(bg3)
                                        }
                                    }.clickable {
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

                        val isInstantVisible = selectedTab == 0 && uiState !is DownloadState.Downloading
                        val instantFabSize by animateDpAsState(if (isInstantVisible) 52.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                        val instantSpacerSize by animateDpAsState(if (isInstantVisible) 10.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                        
                        if (instantFabSize > 0.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(instantSpacerSize))
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text(stringResource(R.string.instant_download)) } },
                                    state = rememberTooltipState()
                                ) {
                                    FloatingActionButton(
                                        onClick = {
                                            clipboardManager.getText()?.let { 
                                                val text = it.text
                                                url = text
                                                viewModel.fetchPreview(text, quality, downloadMode, engine)
                                                viewModel.downloadMedia(text, quality, downloadMode, engine)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(instantFabSize)
                                            .graphicsLayer {
                                                scaleX = instantScale
                                                scaleY = instantScale
                                            },
                                        interactionSource = instantInteractionSource,
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = "Instant Download")
                                    }
                                }
                            }
                        }
                        
                        val isVoiceVisible = selectedTab == 1
                        val voiceFabSize by animateDpAsState(if (isVoiceVisible) 52.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                        val voiceSpacerSize by animateDpAsState(if (isVoiceVisible) 10.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                        
                        if (voiceFabSize > 0.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(voiceSpacerSize))
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text(stringResource(R.string.voice_search)) } },
                                    state = rememberTooltipState()
                                ) {
                                    FloatingActionButton(
                                        onClick = {
                                            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            }
                                            try {
                                                speechRecognizerLauncher.launch(intent)
                                            } catch (e: android.content.ActivityNotFoundException) {
                                                showSpeechNotSupportedDialog = true
                                            }
                                        },
                                        modifier = Modifier
                                            .size(voiceFabSize)
                                            .graphicsLayer {
                                                scaleX = voiceScale
                                                scaleY = voiceScale
                                            },
                                        interactionSource = voiceInteractionSource,
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                                    }
                                }
                            }
                        }
                        
                        val isBinVisible = selectedTab == 2 && history.isNotEmpty()
                        val binFabSize by animateDpAsState(if (isBinVisible) 52.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                        val binSpacerSize by animateDpAsState(if (isBinVisible) 10.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))

                        if (binFabSize > 0.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(binSpacerSize))
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text(stringResource(R.string.clear_history_1)) } },
                                    state = rememberTooltipState()
                                ) {
                                    FloatingActionButton(
                                        onClick = { showClearLogsDialog = true },
                                        modifier = Modifier
                                            .size(binFabSize)
                                            .graphicsLayer {
                                                scaleX = clearLogsScale
                                                scaleY = clearLogsScale
                                            },
                                        interactionSource = clearLogsInteractionSource,
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Logs")
                                    }
                                }
                            }
                        }
                        val isSearchVisible = selectedTab == 3
                        val searchFabSize by animateDpAsState(if (isSearchVisible) 52.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                        val searchSpacerSize by animateDpAsState(if (isSearchVisible) 10.dp else 0.dp, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))

                        if (searchFabSize > 0.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(searchSpacerSize))
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text("Search Settings") } },
                                    state = rememberTooltipState()
                                ) {
                                    FloatingActionButton(
                                        onClick = { isSettingsSearchActive = !isSettingsSearchActive; if(!isSettingsSearchActive) settingsSearchQuery = "" },
                                        modifier = Modifier
                                            .size(searchFabSize)
                                            .graphicsLayer {
                                                scaleX = searchScale
                                                scaleY = searchScale
                                            },
                                        interactionSource = searchInteractionSource,
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Icon(if (isSettingsSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search Settings")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenMargin = if (isExpanded) (configuration.screenWidthDp * 0.20f).dp else 0.dp
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = screenMargin).haze(state = hazeState)) {
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
                        2 -> LogsTab(
                            history = history, 
                            onDelete = viewModel::deleteLog,
                            contentPadding = padding
                        )
                        3 -> SettingsTab(viewModel, contentPadding = padding, isSearchActive = isSettingsSearchActive, searchQuery = settingsSearchQuery, onQueryChange = { settingsSearchQuery = it })
                    }
                }
            }
        }

        if (showSpeechNotSupportedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeechNotSupportedDialog = false },
                title = { Text(stringResource(R.string.voice_search_not_supported)) },
                text = { Text(stringResource(R.string.your_device_does_not_have_a_bu)) },
                confirmButton = {
                    TextButton(onClick = { showSpeechNotSupportedDialog = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }

        if (showSupportedSitesDialog) {
            AlertDialog(
                onDismissRequest = { showSupportedSitesDialog = false },
                title = { Text(stringResource(R.string.supported_sites), style = MaterialTheme.typography.titleLarge) },
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
                                    shape = RoundedCornerShape(24.dp)
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
                                    shape = RoundedCornerShape(24.dp),
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
                        Text(stringResource(R.string.close))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTipsDialog = true }) {
                        Text(stringResource(R.string.help_me))
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
        
        if (showTipsDialog) {
            AlertDialog(
                onDismissRequest = { showTipsDialog = false },
                title = { Text(stringResource(R.string.downloading_tips)) },
                text = {
                    Text(stringResource(R.string.use_newpipe_for_youtube_downlo) +
                        "• Use yt-dlp as a fallback (audio extraction might take some time, so be patient).\n" +
                        "• For Pinterest and similar short-video/social sites, use Cobalt.\n" +
                        "• For images and galleries, use gallery-dl.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTipsDialog = false }) {
                        Text(stringResource(R.string.got_it))
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
    var showTerminalCard by remember { mutableStateOf(viewModel.getSetting("show_terminal_card", "true").toBoolean()) }
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

    LaunchedEffect(showTerminalCard) {
        viewModel.saveSetting("show_terminal_card", showTerminalCard.toString())
    }

    if (duplicateWarningUrl != null) {
        AlertDialog(
            onDismissRequest = { duplicateWarningUrl = null },
            title = { Text(stringResource(R.string.duplicate_download)) },
            text = { Text(stringResource(R.string.you_have_already_downloaded_th)) },
            confirmButton = {
                TextButton(onClick = {
                    val proceedUrl = duplicateWarningUrl!!
                    duplicateWarningUrl = null
                    viewModel.downloadMedia(proceedUrl, quality, downloadMode, engine)
                }) {
                    Text(stringResource(R.string.download_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateWarningUrl = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    var showCancelConfirmationDialog by remember { mutableStateOf(false) }
    var showDownloadingDetailsSheet by remember { mutableStateOf(false) }

    if (showCancelConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmationDialog = false },
            title = { Text(stringResource(R.string.cancel_download)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_to_cance)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmationDialog = false
                    viewModel.cancelDownload()
                }) {
                    Text(stringResource(R.string.yes_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmationDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (showDownloadingDetailsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDownloadingDetailsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.downloading), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                if (preview != null) {
                    AsyncImage(
                        model = preview?.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(preview?.title ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }

                if (uiState is DownloadState.Downloading) {
                    val state = uiState as DownloadState.Downloading
                    val prog = state.progress
                    val downloadedMb = state.downloadedBytes / 1_048_576f
                    val totalMb = state.totalBytes / 1_048_576f
                    val speedKbps = state.speedBps / 1024f
                    
                    WavyProgressIndicator(
                        progress = prog,
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        waveAmplitude = 3.dp,
                        waveFrequency = 0.05f
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (state.totalBytes > 0) String.format("%.1f MB / %.1f MB", downloadedMb, totalMb) else String.format("%.1f MB downloaded", downloadedMb), style = MaterialTheme.typography.bodySmall)
                        Text(String.format("%.1f KB/s", speedKbps), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.cancelDownload()
                        showDownloadingDetailsSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cancel_download_1))
                }
            }
        }
    }

    viewModel.updateAvailable.value?.let { updateInfo ->
        AlertDialog(
            onDismissRequest = { viewModel.updateAvailable.value = null },
            title = { Text("Update Available: ${updateInfo.first}") },
            text = { Text(stringResource(R.string.a_new_version_of_gabi_is_avail)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateAvailable.value = null
                    com.material.downloader.util.AppUpdater(context).downloadAndInstallUpdate(updateInfo.second)
                }) {
                    Text(stringResource(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateAvailable.value = null }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 108.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.gabi), 
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
            placeholder = { Text(stringResource(R.string.search_or_paste_link)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(YoutubeOutline, null, modifier = Modifier.size(20.dp)) },
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
                                            shape = RoundedCornerShape(24.dp),
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
                                    .clip(RoundedCornerShape(24.dp))
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
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
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
                                        shape = RoundedCornerShape(24.dp),
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
                                val videoUri = android.net.Uri.parse(streamUrlToPlay)
                                val videoSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                                    .createMediaSource(androidx.media3.common.MediaItem.fromUri(videoUri))
                                
                                val audioUrl = meta.audio_url
                                if (!audioUrl.isNullOrBlank()) {
                                    val audioSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                                        .createMediaSource(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(audioUrl)))
                                    val mergedSource = androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
                                    exoPlayer.setMediaSource(mergedSource)
                                } else {
                                    exoPlayer.setMediaSource(videoSource)
                                }
                                
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
                                        val view = android.view.LayoutInflater.from(ctx).inflate(com.material.downloader.R.layout.player_view_layout, null) as androidx.media3.ui.PlayerView
                                        view.apply {
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
                        val isImageOrGallery = downloadMode == "image" || meta.ext in listOf("jpg", "jpeg", "png", "webp", "gif") || engine == "gallery-dl"
                        
                        if (!isImageOrGallery) {
                            Button(
                                onClick = { }, // Just informational in this context
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(if (isAudio) Icons.Default.MusicNote else Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                val qual = if (quality == "best") "Best" else "${quality}p"
                                Text(if (isAudio) "Audio" else "Video ($qual)", maxLines = 1)
                            }
                        }

                        Button(
                            onClick = { 
                                showPreviewSheet = false
                                viewModel.downloadMedia(url = url, quality = quality, mode = downloadMode, engine = engine)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.download))
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
                val engineColors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
                val engineInnerCorner by animateDpAsState(if (engineExpanded) 100.dp else 8.dp)
                FilledTonalButton(
                    onClick = { engineExpanded = true },
                    shape = RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp, topEnd = engineInnerCorner, bottomEnd = engineInnerCorner),
                    colors = engineColors,
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
                        text = { Text(stringResource(R.string.newpipe)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp)) },
                        onClick = { 
                            onEngineChange("newpipe")
                            engineExpanded = false
                            viewModel.clearPreview()
                            viewModel.logToConsole("Switched engine to: NewPipe")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.yt_dlp)) },
                        leadingIcon = { Icon(Icons.Default.Movie, null, modifier = Modifier.size(18.dp)) },
                        onClick = { 
                            onEngineChange("yt-dlp")
                            engineExpanded = false
                            viewModel.clearPreview()
                            viewModel.logToConsole("Switched engine to: yt-dlp")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.gallery_dl)) },
                        leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) },
                        onClick = { 
                            onEngineChange("gallery-dl")
                            engineExpanded = false
                            viewModel.clearPreview()
                            viewModel.logToConsole("Switched engine to: gallery-dl")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cobalt)) },
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
                val isModeDefault = downloadMode == "auto"
                val modeColors = if (isModeDefault) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
                val modeCorner by animateDpAsState(if (modeExpanded) 100.dp else 8.dp)
                FilledTonalButton(
                    onClick = { if (engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe") modeExpanded = true },
                    shape = RoundedCornerShape(modeCorner),
                    colors = modeColors,
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
                val isQualityDefault = quality == "720"
                val qualityColors = if (isQualityDefault) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
                val qualityInnerCorner by animateDpAsState(if (qualityExpanded) 100.dp else 8.dp)
                FilledTonalButton(
                    onClick = { if (engine == "yt-dlp" || engine == "cobalt" || engine == "newpipe") qualityExpanded = true },
                    shape = RoundedCornerShape(topStart = qualityInnerCorner, bottomStart = qualityInnerCorner, topEnd = 100.dp, bottomEnd = 100.dp),
                    colors = qualityColors,
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

        // Connected Button Group (Terminal / Download)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Terminal Pill Button (Left)
            Box(modifier = Modifier.weight(1f)) {
                val terminalInteractionSource = remember { MutableInteractionSource() }
                val isTerminalPressed by terminalInteractionSource.collectIsPressedAsState()
                val terminalInnerCorner by animateDpAsState(if (isTerminalPressed || showTerminalCard) 100.dp else 8.dp)
                
                val terminalContainerColor by animateColorAsState(
                    targetValue = if (showTerminalCard) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(150),
                    label = "terminal_container_color"
                )
                val terminalContentColor by animateColorAsState(
                    targetValue = if (showTerminalCard) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(150),
                    label = "terminal_content_color"
                )

                FilledTonalButton(
                    onClick = { showTerminalCard = !showTerminalCard },
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = terminalInteractionSource,
                    shape = RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp, topEnd = terminalInnerCorner, bottomEnd = terminalInnerCorner),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = terminalContainerColor,
                        contentColor = terminalContentColor
                    ),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.terminal), maxLines = 1)
                }
            }

            // Download Pill Button (Right)
            Box(modifier = Modifier.weight(1f)) {
                val downloadInteractionSource = remember { MutableInteractionSource() }
                val isDownloadPressed by downloadInteractionSource.collectIsPressedAsState()
                val isDownloading = uiState is DownloadState.Downloading
                val downloadInnerCorner by animateDpAsState(if (isDownloadPressed || isDownloading) 100.dp else 8.dp)
                val downloadContainerColor by animateColorAsState(
                    targetValue = if (isDownloading) MaterialTheme.colorScheme.primaryContainer else if (isDownloadPressed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                    animationSpec = tween(150),
                    label = "download_container_color"
                )
                val downloadContentColor by animateColorAsState(
                    targetValue = if (isDownloading) MaterialTheme.colorScheme.onPrimaryContainer else if (isDownloadPressed) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
                    animationSpec = tween(150),
                    label = "download_content_color"
                )

                FilledTonalButton(
                    onClick = { 
                        if (isDownloading) {
                            showDownloadingDetailsSheet = true
                        } else if (url.isNotBlank()) {
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
                        .combinedClickable(
                            onClick = {
                                if (isDownloading) {
                                    showDownloadingDetailsSheet = true
                                } else if (url.isNotBlank()) {
                                    coroutineScope.launch {
                                        if (viewModel.hasDownloadedUrl(url)) {
                                            duplicateWarningUrl = url
                                        } else {
                                            viewModel.downloadMedia(url, quality, downloadMode, engine)
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                if (isDownloading) {
                                    showDownloadingDetailsSheet = true
                                }
                            }
                        ),
                    interactionSource = downloadInteractionSource,
                    shape = RoundedCornerShape(topStart = downloadInnerCorner, bottomStart = downloadInnerCorner, topEnd = 100.dp, bottomEnd = 100.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = downloadContainerColor,
                        contentColor = downloadContentColor
                    ),
                    contentPadding = if (isDownloading) PaddingValues(horizontal = 10.dp, vertical = 14.dp) else PaddingValues(14.dp)
                ) {
                    if (isDownloading) {
                        val progress = (uiState as DownloadState.Downloading).progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WavyProgressIndicator(
                                progress = progress,
                                modifier = Modifier.weight(1f).height(12.dp),
                                strokeWidth = 3.dp,
                                waveAmplitude = 2.dp,
                                color = downloadContentColor,
                                trackColor = downloadContentColor.copy(alpha = 0.25f)
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    } else {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.download), maxLines = 1)
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState is DownloadState.Success || uiState is DownloadState.Error) {
            StatusInfo(uiState, viewModel::openSavedFolder)
        }

        // Terminal CLI Component
        AnimatedVisibility(
            visible = showTerminalCard,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
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
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.download_again))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetDownloadState()
                    }
                ) {
                    Text(stringResource(R.string.close))
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.downloading), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            WavyProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    waveAmplitude: androidx.compose.ui.unit.Dp = 3.dp,
    waveFrequency: Float = 0.08f,
    strokeWidth: androidx.compose.ui.unit.Dp = 6.dp,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * kotlin.math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val amplitude = waveAmplitude.toPx()
        val freq = waveFrequency
        
        val filledWidth = w * progress

        val lastY = if (filledWidth > 0f) centerY + amplitude * kotlin.math.sin(filledWidth * freq - phase) else centerY

        // Draw track connected to the end of the wave and tapered to center
        if (filledWidth < w) {
            val trackPath = androidx.compose.ui.graphics.Path()
            trackPath.moveTo(filledWidth, lastY)
            val distanceToCenter = kotlin.math.min(40f, w - filledWidth)
            if (distanceToCenter > 0f) {
                trackPath.cubicTo(
                    filledWidth + distanceToCenter / 2, lastY,
                    filledWidth + distanceToCenter / 2, centerY,
                    filledWidth + distanceToCenter, centerY
                )
            }
            trackPath.lineTo(w, centerY)

            drawPath(
                path = trackPath,
                color = trackColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }

        // Draw wavy filled part
        if (filledWidth > 0f) {
            val path = androidx.compose.ui.graphics.Path()
            var first = true
            for (x in 0..filledWidth.toInt() step 2) {
                val y = centerY + amplitude * kotlin.math.sin(x * freq - phase)
                if (first) {
                    path.moveTo(x.toFloat(), y)
                    first = false
                } else {
                    path.lineTo(x.toFloat(), y)
                }
            }
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth.toPx(), 
                    cap = androidx.compose.ui.graphics.StrokeCap.Round, 
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun SegmentCard(
    onClick: () -> Unit,
    defaultTopStart: androidx.compose.ui.unit.Dp = 4.dp,
    defaultTopEnd: androidx.compose.ui.unit.Dp = 4.dp,
    defaultBottomStart: androidx.compose.ui.unit.Dp = 4.dp,
    defaultBottomEnd: androidx.compose.ui.unit.Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val topStart by animateDpAsState(if (isPressed) 100.dp else defaultTopStart, animationSpec = tween(150), label = "topStart")
    val topEnd by animateDpAsState(if (isPressed) 100.dp else defaultTopEnd, animationSpec = tween(150), label = "topEnd")
    val bottomStart by animateDpAsState(if (isPressed) 100.dp else defaultBottomStart, animationSpec = tween(150), label = "bottomStart")
    val bottomEnd by animateDpAsState(if (isPressed) 100.dp else defaultBottomEnd, animationSpec = tween(150), label = "bottomEnd")
    
    val cardContainerColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(150),
        label = "card_color"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomStart = bottomStart, bottomEnd = bottomEnd),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor)
    ) {
        content()
    }
}

@Composable
fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    useMaterialShapes: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shapesList = remember {
            listOf(
                androidx.graphics.shapes.RoundedPolygon(numVertices = 8, rounding = androidx.graphics.shapes.CornerRounding(1f)), // Circle / Octagon
                androidx.graphics.shapes.RoundedPolygon(numVertices = 6, rounding = androidx.graphics.shapes.CornerRounding(0.2f)), // Hexagon
                androidx.graphics.shapes.RoundedPolygon(numVertices = 4, rounding = androidx.graphics.shapes.CornerRounding(0.3f)), // Rounded Square
                androidx.graphics.shapes.RoundedPolygon(numVertices = 5, rounding = androidx.graphics.shapes.CornerRounding(0.3f)), // Rounded Pentagon
                androidx.graphics.shapes.RoundedPolygon(numVertices = 3, rounding = androidx.graphics.shapes.CornerRounding(0.2f)) // Trigance
            )
        }
        val randomPolygon = remember { shapesList.random() }
        val expressiveShape = remember(randomPolygon) { PolygonShape(randomPolygon) }
        val finalShape = if (useMaterialShapes) expressiveShape else CircleShape

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconColor, shape = finalShape),
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
fun SettingsTab(viewModel: DownloaderViewModel, contentPadding: PaddingValues = PaddingValues(0.dp), isSearchActive: Boolean = false, searchQuery: String = "", onQueryChange: (String) -> Unit = {}) {
    var currentScreen by remember { mutableStateOf("Main") }

    BackHandler(enabled = currentScreen != "Main") {
        currentScreen = "Main"
    }
    
    LaunchedEffect(isSearchActive) {
        if (isSearchActive && currentScreen != "Main") {
            currentScreen = "Main"
        }
    }

    // currentScreen format: "ScreenName" or "ScreenName?target=CardTitle"
    val screenName = currentScreen.substringBefore("?")
    val target = if (currentScreen.contains("?target=")) currentScreen.substringAfter("?target=") else null

    AnimatedContent(
        targetState = screenName,
        transitionSpec = {
            if (targetState != "Main") {
                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it } + fadeOut()
                )
            } else {
                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it } + fadeIn()).togetherWith(
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } + fadeOut()
                )
            }.using(SizeTransform(clip = false))
        },
        label = "settings_nav"
    ) { screen ->
        when (screen) {
            "Main" -> SettingsMainList(
                viewModel = viewModel, 
                onNavigate = { currentScreen = it }, 
                contentPadding = contentPadding,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange
            )
            "Customisation" -> CustomisationScreen(viewModel, onBack = { currentScreen = "Main" }, contentPadding, target)
            "Downloads" -> DownloadsSettingsScreen(viewModel, onBack = { currentScreen = "Main" }, contentPadding, target)
            "Cookies" -> CookiesSettingsScreen(viewModel, onBack = { currentScreen = "Main" }, contentPadding, target)
            "Developer" -> DeveloperScreen(viewModel = viewModel, onBack = { currentScreen = "Main" }, contentPadding, target)
            "Support" -> SupportScreen(onBack = { currentScreen = "Main" }, contentPadding, target)
        }
    }
}

@Composable
fun SettingsMainList(
    viewModel: DownloaderViewModel, 
    onNavigate: (String) -> Unit, 
    contentPadding: PaddingValues,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    val allSettings = remember {
        listOf(
            Triple("Navigation Bar Blur", "Enable haze effect on floating navigation bar", "Customisation?target=Navigation Bar Blur"),
            Triple("True Glass Navigation", "Make navigation bar fully transparent glass", "Customisation?target=True Glass Navigation"),
            Triple("Opaque Navigation Bar", "Make floating navigation solid color", "Customisation?target=Opaque Navigation Bar"),
            Triple("Material Expressive Shapes", "Use dynamic shapes for settings icons", "Customisation?target=Material Expressive Shapes"),
            Triple("Theme Mode", "System, Light, Dark", "Customisation?target=Theme Mode"),
            Triple("Color Scheme", "App Colors", "Customisation?target=Color Scheme"),
            Triple("Language", "Change app language", "Customisation?target=Language"),
            Triple("Download Repository", "Files will be saved in your selected folder", "Downloads?target=Download Repository"),
            Triple("Extractors", "Update internal components", "Downloads?target=Extractors"),
            Triple("Cookies", "Extraction Credentials", "Cookies?target=Cookies"),
            Triple("Check for Updates", "Check latest GitHub release", "Developer?target=Check for Updates"),
            Triple("Support", "Support on Ko-fi, UPI, GitHub", "Support?target=Support")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()).padding(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = isSearchActive,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search settings...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(100),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )
        }

        if (isSearchActive && searchQuery.isNotEmpty()) {
            val results = allSettings.filter { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                results.forEach { item ->
                    Card(
                        onClick = {
                            onQueryChange("")
                            onNavigate(item.third)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(item.first) },
                            supportingContent = { Text(item.second) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        } else {
        
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SegmentCard(
                onClick = { onNavigate("Customisation") },
                defaultTopStart = 24.dp,
                defaultTopEnd = 24.dp,
                defaultBottomStart = 4.dp,
                defaultBottomEnd = 4.dp
            ) {
                SettingsListItem(
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFF88637B), // Mauve
                    title = "Customisation",
                    subtitle = "Themes, App Appearance, Terminal",
                    useMaterialShapes = viewModel.isMaterialShapesEnabled.value
                )
            }

            SegmentCard(
                onClick = { onNavigate("Downloads") },
                defaultTopStart = 4.dp,
                defaultTopEnd = 4.dp,
                defaultBottomStart = 4.dp,
                defaultBottomEnd = 4.dp
            ) {
                SettingsListItem(
                    icon = Icons.Default.FolderOpen,
                    iconColor = Color(0xFF00758F), // Teal
                    title = "Downloads",
                    subtitle = "Storage location, rules",
                    useMaterialShapes = viewModel.isMaterialShapesEnabled.value
                )
            }

            SegmentCard(
                onClick = { onNavigate("Cookies") },
                defaultTopStart = 4.dp,
                defaultTopEnd = 4.dp,
                defaultBottomStart = 4.dp,
                defaultBottomEnd = 4.dp
            ) {
                SettingsListItem(
                    icon = Icons.Default.Cookie,
                    iconColor = Color(0xFFD4A373), // Cookie color
                    title = "Cookies",
                    subtitle = "Manage authentication cookies",
                    useMaterialShapes = viewModel.isMaterialShapesEnabled.value
                )
            }

            SegmentCard(
                onClick = { onNavigate("Developer") },
                defaultTopStart = 4.dp,
                defaultTopEnd = 4.dp,
                defaultBottomStart = 4.dp,
                defaultBottomEnd = 4.dp
            ) {
                SettingsListItem(
                    icon = Icons.Default.Code,
                    iconColor = Color(0xFF4C6B8B), // Slate blue
                    title = "Developer",
                    subtitle = "App Info, Platforms, Licenses",
                    useMaterialShapes = viewModel.isMaterialShapesEnabled.value
                )
            }

            SegmentCard(
                onClick = { onNavigate("Support") },
                defaultTopStart = 4.dp,
                defaultTopEnd = 4.dp,
                defaultBottomStart = 24.dp,
                defaultBottomEnd = 24.dp
            ) {
                SettingsListItem(
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFF7A4F5C), // Burgundy
                    title = "Support",
                    subtitle = "Help keep Gabi alive",
                    useMaterialShapes = viewModel.isMaterialShapesEnabled.value
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomisationScreen(viewModel: DownloaderViewModel, onBack: () -> Unit, contentPadding: PaddingValues, target: String? = null) {
    val scrollState = rememberScrollState()
    val positions = remember { mutableMapOf<String, Float>() }

    LaunchedEffect(target) {
        if (target != null) {
            kotlinx.coroutines.delay(300)
            val y = positions[target]
            if (y != null) {
                scrollState.animateScrollTo(y.toInt())
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState).padding(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.customisation), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.navigation_bar_blur), style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["Navigation Bar Blur"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                        Text(stringResource(R.string.enable_haze_effect_on_floating), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.isNavBarBlurEnabled.value,
                        onCheckedChange = { viewModel.toggleNavBarBlur(it) }
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.true_glass_navigation), style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["True Glass Navigation"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                        Text(stringResource(R.string.make_navigation_bar_fully_tran), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.isNavBarTrueGlass.value,
                        onCheckedChange = { viewModel.toggleNavBarTrueGlass(it) }
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.opaque_navigation_bar), style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["Opaque Navigation Bar"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                        Text(stringResource(R.string.make_floating_navigation_solid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.isNavBarOpaque.value,
                        onCheckedChange = { viewModel.toggleNavBarOpaque(it) }
                    )
                }
            }
        }
        

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Language / اللغة / भाषा", style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["Language"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val languages = listOf(
                        "en" to "English",
                        "ar" to "العربية",
                        "hi" to "हिन्दी",
                        "ru" to "Русский",
                        "ja" to "日本語",
                        "zh" to "中文",
                        "de" to "Deutsch",
                        "ne" to "नेपाली",
                        "bn" to "বাংলা"
                    )
                    val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
                    
                    languages.chunked(3).forEach { rowLangs ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            rowLangs.forEachIndexed { index, lang ->
                                val isSelected = currentLocale == lang.first || (currentLocale == "en" && lang.first == "en")
                                val shape = RoundedCornerShape(
                                    topStart = if (index == 0) 100.dp else 8.dp,
                                    bottomStart = if (index == 0) 100.dp else 8.dp,
                                    topEnd = if (index == rowLangs.lastIndex) 100.dp else 8.dp,
                                    bottomEnd = if (index == rowLangs.lastIndex) 100.dp else 8.dp
                                )
                                val colors = if (isSelected) {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                } else {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                            androidx.core.os.LocaleListCompat.forLanguageTags(lang.first)
                                        )
                                    },
                                    shape = shape,
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(lang.second, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            if (rowLangs.size < 3) {
                                repeat(3 - rowLangs.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.material_expressive_shapes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["Material Expressive Shapes"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                        Text(stringResource(R.string.use_dynamic_shapes_for_setting), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.isMaterialShapesEnabled.value,
                        onCheckedChange = { viewModel.toggleMaterialShapes(it) }
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["Theme Mode"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val options = listOf("System", "Light", "Dark")
                    options.forEachIndexed { index, label ->
                        val isSelected = viewModel.themeMode.intValue == index
                        val shape = RoundedCornerShape(
                            topStart = if (index == 0) 100.dp else 8.dp,
                            bottomStart = if (index == 0) 100.dp else 8.dp,
                            topEnd = if (index == options.lastIndex) 100.dp else 8.dp,
                            bottomEnd = if (index == options.lastIndex) 100.dp else 8.dp
                        )
                        val colors = if (isSelected) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.updateThemeMode(index) },
                            shape = shape,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
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
                Text(stringResource(R.string.color_scheme), style = MaterialTheme.typography.titleMedium, modifier = Modifier.onGloballyPositioned { positions["Color Scheme"] = it.parentLayoutCoordinates?.parentLayoutCoordinates?.positionInParent()?.y ?: 0f })
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = AppTheme.values().toList()
                    themes.chunked(3).forEach { rowThemes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            rowThemes.forEachIndexed { index, theme ->
                                val isSelected = viewModel.selectedTheme.value == theme
                                val shape = RoundedCornerShape(
                                    topStart = if (index == 0) 100.dp else 8.dp,
                                    bottomStart = if (index == 0) 100.dp else 8.dp,
                                    topEnd = if (index == rowThemes.lastIndex) 100.dp else 8.dp,
                                    bottomEnd = if (index == rowThemes.lastIndex) 100.dp else 8.dp
                                )
                                val colors = if (isSelected) {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                } else {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.updateSelectedTheme(theme) },
                                    shape = shape,
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(theme.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            // Fill remaining space if chunk is smaller than 3
                            if (rowThemes.size < 3) {
                                repeat(3 - rowThemes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
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
                Text(stringResource(R.string.terminal_theme), style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = TerminalTheme.values().toList()
                    themes.chunked(2).forEach { rowThemes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            rowThemes.forEachIndexed { index, theme ->
                                val isSelected = viewModel.terminalTheme.value == theme
                                val shape = RoundedCornerShape(
                                    topStart = if (index == 0) 100.dp else 8.dp,
                                    bottomStart = if (index == 0) 100.dp else 8.dp,
                                    topEnd = if (index == rowThemes.lastIndex) 100.dp else 8.dp,
                                    bottomEnd = if (index == rowThemes.lastIndex) 100.dp else 8.dp
                                )
                                val colors = if (isSelected) {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                } else {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.updateTerminalTheme(theme) },
                                    shape = shape,
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(theme.displayName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            if (rowThemes.size < 2) {
                                repeat(2 - rowThemes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsSettingsScreen(viewModel: DownloaderViewModel, onBack: () -> Unit, contentPadding: PaddingValues, target: String? = null) {
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
            onCookiesExtracted = { extractedUrl, cookies ->
                val engine = targetEngineForCookies!!
                val uri = android.net.Uri.parse(extractedUrl)
                val domain = uri.host ?: "Unknown Site"
                val session = com.material.downloader.model.CookieSession(
                    id = java.util.UUID.randomUUID().toString(),
                    domain = domain,
                    cookieString = cookies,
                    engine = engine
                )
                viewModel.addCookieSession(session)
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
            title = { Text(stringResource(R.string.select_target_extractor)) },
            text = { Text(stringResource(R.string.which_extractor_do_you_want_to)) },
            confirmButton = {
                TextButton(onClick = {
                    showEngineChooser = false
                    targetEngineForCookies = "yt_dlp"
                    if (chooserAction == "import") cookiesPickerLauncher.launch(arrayOf("text/plain"))
                    else showCookieExtractor = true
                }) { Text(stringResource(R.string.yt_dlp)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEngineChooser = false
                    targetEngineForCookies = "gallery_dl"
                    if (chooserAction == "import") cookiesPickerLauncher.launch(arrayOf("text/plain"))
                    else showCookieExtractor = true
                }) { Text(stringResource(R.string.gallery_dl)) }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()).padding(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.downloads), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Text(stringResource(R.string.download_repository), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.files_will_be_saved_in_your_se), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
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
                        Text(stringResource(R.string.default_movies_gabi), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.extractors), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.update_internal_components), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
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
                    Text(stringResource(R.string.update_yt_dlp_gallery_dl), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("yt-dlp: $ytDlpVersion • gallery-dl: $galleryDlVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.extraction_credentials), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.import_cookies_txt_for_private), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
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
                    Text(stringResource(R.string.import_cookies_txt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (ytCookies) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.yt_dlp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (galleryCookies) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.gallery_dl), style = MaterialTheme.typography.bodySmall)
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
                    Text(stringResource(R.string.extract_from_web_login), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.login_to_sites_to_get_cookies), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            val domainWithDot = if (domain.startsWith(".")) domain else ".$domain"
            out.append("$domainWithDot\tTRUE\t/\tFALSE\t2147483647\t$name\t$value\n")
        }
    }
    cookiesFile.writeText(out.toString())
    return cookiesFile.absolutePath
}

@Composable
fun CookieExtractorDialog(initialUrl: String = "https://youtube.com", fileName: String, onDismiss: () -> Unit, onCookiesExtracted: (String, String) -> Unit) {
    var url by remember { mutableStateOf(initialUrl) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
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
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
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
                            onCookiesExtracted(currentUrl, cookies)
                            onDismiss()
                        } else {
                            android.widget.Toast.makeText(context, "No cookies found", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(stringResource(R.string.extract_save_cookies))
                }
            }
        }
    }
}
@Composable
fun DeveloperScreen(viewModel: DownloaderViewModel, onBack: () -> Unit, contentPadding: PaddingValues, target: String? = null) {
    val context = LocalContext.current
    val profileImageRequest = remember(context) {
        coil.request.ImageRequest.Builder(context)
            .data("https://github.com/Hotaro26.png")
            .diskCacheKey("hotaro_avatar")
            .memoryCacheKey("hotaro_avatar")
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()).padding(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.developer), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                            model = profileImageRequest,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text(stringResource(R.string.hotaro), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.building_crisp_fast_and_secure), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hotaro26"))) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(id = com.material.downloader.R.drawable.ic_github), null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.github))
                    }
                    OutlinedButton(
                        onClick = { Toast.makeText(context, "Discord: oi.hotaro", Toast.LENGTH_LONG).show() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(id = com.material.downloader.R.drawable.ic_discord), null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.discord))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.app_info_updates), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gabi v${com.material.downloader.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.powered_by_yt_dlp_gallery_dl_c), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.auto_check_updates), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.notify_on_launch_when_new_rele), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.autoCheckUpdates.value,
                        onCheckedChange = { viewModel.toggleAutoCheckUpdates(it) }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.check_for_updates), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        viewModel.updateCheckMessage.value?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } ?: Text(stringResource(R.string.check_latest_github_release), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.checkForUpdates(manual = true) },
                        enabled = !viewModel.isCheckingUpdates.value,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (viewModel.isCheckingUpdates.value) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(R.string.check))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit, contentPadding: PaddingValues, target: String? = null) {
    val context = LocalContext.current
    val myUpiId = "9693703723@fam"
    var showKofiDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
    
    if (showKofiDialog) {
        AlertDialog(
            onDismissRequest = { showKofiDialog = false },
            title = { Text(stringResource(R.string.support_on_ko_fi), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.kofi_qr),
                        contentDescription = "Ko-fi QR Code",
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp))
                    )
                    OutlinedCard(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/oi.hotaro")))
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.ko_fi_com_oi_hotaro), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKofiDialog = false }) { Text(stringResource(R.string.close)) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()).padding(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.support), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.support_gabi_development), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.help_keep_gabi_alive_and_fast),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.upi))
                    }
                    Button(
                        onClick = { showKofiDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF00B9FE))
                    ) {
                        Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ko_fi))
                    }
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
                    Text(stringResource(R.string.star_the_project), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.support_on_github), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTab(
    history: List<com.material.downloader.model.DownloadLog>, 
    onDelete: (com.material.downloader.model.DownloadLog) -> Unit,
    onItemClick: (com.material.downloader.model.DownloadLog) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var itemToDelete by remember { mutableStateOf<com.material.downloader.model.DownloadLog?>(null) }
    var selectedLog by remember { mutableStateOf<com.material.downloader.model.DownloadLog?>(null) }
    val context = LocalContext.current
    
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.clear_history_1)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_to_remov)) },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { onDelete(it) }
                    itemToDelete = null
                }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp + contentPadding.calculateTopPadding())
    ) {
        Text(stringResource(R.string.recent_downloads), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_history_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp + contentPadding.calculateBottomPadding())
            ) {
                items(history, key = { it.id }) { log ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                itemToDelete = log
                            }
                            false // always snap back, delete handled by dialog
                        }
                    )
                    
                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedLog = log }, 
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), 
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val thumbPath = log.thumbnailPath ?: log.path
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!thumbPath.isNullOrEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = thumbPath,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (log.status == "Success") Icons.Default.PlayArrow else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            log.title, 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1, 
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            log.status, 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = if (log.status == "Success") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedLog != null && selectedLog?.path != null) {
        val log = selectedLog!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val ext = log.path?.substringAfterLast('.', "") ?: ""
        val isGallery = ext.equals("jpg", true) || ext.equals("png", true) || ext.equals("jpeg", true) || ext.equals("webp", true) || ext.equals("gif", true) || (log.title.contains("Gallery", true) && !ext.equals("mp4", true))

        ModalBottomSheet(
            onDismissRequest = { selectedLog = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isGallery) {
                    coil.compose.AsyncImage(
                        model = log.path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(300.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    val streamUrlToPlay = log.path ?: ""
                    if (streamUrlToPlay.isNotBlank()) {
                        val exoPlayer = remember { androidx.media3.exoplayer.ExoPlayer.Builder(context).build() }
                        DisposableEffect(streamUrlToPlay) {
                            val videoUri = android.net.Uri.parse(streamUrlToPlay)
                            val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                            onDispose { exoPlayer.release() }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .background(androidx.compose.ui.graphics.Color.Black)
                        ) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    val view = android.view.LayoutInflater.from(ctx).inflate(com.material.downloader.R.layout.player_view_layout, null) as androidx.media3.ui.PlayerView
                                    view.apply {
                                        player = exoPlayer
                                        useController = true
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                
                Text(log.title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                
                if (log.author != null) {
                    Text(log.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(log.url) })
                        android.widget.Toast.makeText(context, "URL Copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Link", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.copy_original_url), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Button(
                    onClick = { selectedLog = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.close))
                }
                Spacer(Modifier.height(16.dp))
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
        shape = RoundedCornerShape(24.dp),
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
                    Text(stringResource(R.string.check_your_selected_folder), style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesSettingsScreen(viewModel: DownloaderViewModel, onBack: () -> Unit, contentPadding: PaddingValues, target: String? = null) {
    val context = LocalContext.current
    var showNewCookieSheet by remember { mutableStateOf(false) }
    
    // For the extractor dialog
    var showCookieExtractor by remember { mutableStateOf(false) }
    var targetEngineForCookies by remember { mutableStateOf<String?>(null) }
    var extractUrl by remember { mutableStateOf("https://") }
    
    val cookieSessions by viewModel.cookieSessions.collectAsState()

    var cookieContentToShow by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    if (cookieContentToShow != null) {
        AlertDialog(
            onDismissRequest = { cookieContentToShow = null },
            title = { Text(cookieContentToShow!!.first) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(cookieContentToShow!!.second, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = { cookieContentToShow = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showCookieExtractor && targetEngineForCookies != null) {
        CookieExtractorDialog(
            initialUrl = extractUrl,
            fileName = "temp", // not used
            onDismiss = { showCookieExtractor = false },
            onCookiesExtracted = { extractedUrl, cookies ->
                val uri = android.net.Uri.parse(extractedUrl)
                val domain = uri.host ?: "Unknown Site"
                val session = com.material.downloader.model.CookieSession(
                    id = java.util.UUID.randomUUID().toString(),
                    domain = domain,
                    cookieString = cookies,
                    engine = targetEngineForCookies!!
                )
                viewModel.addCookieSession(session)
                android.widget.Toast.makeText(context, "Cookies added for ${targetEngineForCookies!!}", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val fromIndex = cookieSessions.indexOfFirst { it.id == fromKey }
        val toIndex = cookieSessions.indexOfFirst { it.id == toKey }
        if (fromIndex != -1 && toIndex != -1) {
            viewModel.reorderCookieSessions(fromIndex, toIndex)
        }
    })
    
    // Let's use LazyColumn for the whole screen
    LazyColumn(
        state = reorderState.listState,
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp + contentPadding.calculateTopPadding(), bottom = 24.dp + contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) { Icon(Icons.Default.ArrowBack, "Back") }
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.cookies), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.use_cookies), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = viewModel.useCookies.value,
                    onCheckedChange = { viewModel.toggleUseCookies(it) }
                )
            }
        }
        
        item {
            OutlinedButton(
                onClick = { showNewCookieSheet = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, "Add")
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.new_cookie))
            }
        }
        
        if (cookieSessions.isNotEmpty()) {
            item {
                Text(stringResource(R.string.saved_cookies_drag_to_reorder), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            }
            
            items(cookieSessions, key = { it.id }) { session ->
                ReorderableItem(reorderState, key = session.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "")
                    CookieItem(
                        session = session,
                        modifier = Modifier
                            .detectReorderAfterLongPress(reorderState)
                            .shadow(elevation, RoundedCornerShape(12.dp)),
                        onDelete = { viewModel.removeCookieSession(session.id) },
                        onView = { cookieContentToShow = session.domain to session.cookieString }
                    )
                }
            }
        }
        
        item {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.cookies_allow_the_extractors_t),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showNewCookieSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tempUrl by remember { mutableStateOf("https://") }
        var selectedTag by remember { mutableStateOf<String?>(null) }
        var tagExpanded by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showNewCookieSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text(stringResource(R.string.url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedTag) {
                            "yt_dlp" -> "yt-dlp"
                            "gallery_dl" -> "gallery-dl"
                            else -> "Select extractor..."
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.target_extractor)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = tagExpanded,
                        onDismissRequest = { tagExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.yt_dlp)) }, onClick = { selectedTag = "yt_dlp"; tagExpanded = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.gallery_dl)) }, onClick = { selectedTag = "gallery_dl"; tagExpanded = false })
                    }
                }
                
                Button(
                    onClick = {
                        showNewCookieSheet = false
                        extractUrl = tempUrl
                        targetEngineForCookies = selectedTag
                        showCookieExtractor = true
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = selectedTag != null && tempUrl.isNotBlank()
                ) {
                    Icon(Icons.Default.Cookie, "Cookie")
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.get_cookies))
                }
            }
        }
    }
}
@Composable
fun CookieItem(session: com.material.downloader.model.CookieSession, modifier: Modifier = Modifier, onDelete: () -> Unit, onView: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth().clickable { onView() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DragIndicator, contentDescription = "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(session.domain, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val tag = if (session.engine == "yt_dlp") "yt-dlp" else "gallery-dl"
                        Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
