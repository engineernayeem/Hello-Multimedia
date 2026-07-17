package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import android.content.pm.ActivityInfo
import android.app.Activity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.player.PlayerState
import com.example.player.PlayerStats
import com.example.utils.NetworkType
import com.example.ui.viewmodel.IptvViewModel
import com.example.ui.viewmodel.ScreenState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: IptvViewModel,
    onNavigateToSettings: () -> Unit,
    toggleImmersive: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val videoScaling by viewModel.videoScaling.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    val playerManager = viewModel.getPlayerInstance()
    val exoPlayer = playerManager?.getPlayer()

    // Control bar visibility timeout logic
    var showControls by remember { mutableStateOf(true) }
    var showStats by remember { mutableStateOf(false) }

    // Dynamic screen orientation and fullscreen state
    var isFullscreen by remember { mutableStateOf(false) }
    val activity = context as? Activity

    // D-pad focus managers for TV interaction
    val playButtonFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    // Initial focus on root so we can capture D-pad key strokes
    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    // Auto-focus play button when controls are visible, or root box when hidden
    LaunchedEffect(showControls) {
        if (showControls) {
            try {
                playButtonFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures gracefully
            }
        } else {
            try {
                rootFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures gracefully
            }
        }
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Toggle controls visibility and hide after 5s
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // Trigger immersive mode when controls hide/show
    LaunchedEffect(showControls) {
        toggleImmersive(!showControls)
    }

    // Initialize playback if not already active
    LaunchedEffect(Unit) {
        if (playerState == PlayerState.IDLE || playerState == PlayerState.ENDED) {
            viewModel.initializeAndPlay()
        }
    }

    // Handle back stack lifecycle
    DisposableEffect(Unit) {
        onDispose {
            toggleImmersive(false) // Restore UI bars on exiting player screen
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (keyEvent.key == Key.Back) {
                        false
                    } else if (!showControls) {
                        showControls = true
                        true
                    } else {
                        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
                            viewModel.togglePlayPause()
                            true
                        } else {
                            false
                        }
                    }
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        // 1. Video Render Surface (Media3 AndroidView wrapper)
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = exoPlayer
                        resizeMode = when (videoScaling) {
                            1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill Screen
                            2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL  // Stretch
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT // Fit Screen
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = when (videoScaling) {
                        1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. HUD Top Bar Component
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                channelName = currentChannel.name,
                networkType = networkState.type,
                networkConnected = networkState.isConnected,
                onSettingsClick = onNavigateToSettings,
                onToggleStats = { showStats = !showStats },
                isStatsActive = showStats
            )
        }

        // 3. Center Buffering / Error Overlays
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (playerState == PlayerState.BUFFERING) {
                PlayerBufferingOverlay(bufferPercentage = playerStats.bufferPercentage)
            } else if (playerState == PlayerState.ERROR || !networkState.isConnected) {
                PlayerErrorOverlay(
                    errorMessage = errorMessage ?: "Connecting...",
                    reconnectAttempts = playerStats.reconnectCounter,
                    onRetry = { viewModel.reloadStream() }
                )
            }
        }

        // 4. HUD Bottom Bar Control Panel
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerBottomControls(
                playerState = playerState,
                volume = volume,
                isMuted = isMuted,
                playbackSpeed = playbackSpeed,
                videoScaling = videoScaling,
                isFullscreen = isFullscreen,
                playButtonFocusRequester = playButtonFocusRequester,
                onFullscreenToggle = { isFullscreen = !isFullscreen },
                onPlayPause = { viewModel.togglePlayPause() },
                onReload = { viewModel.reloadStream() },
                onStop = { viewModel.stopStream() },
                onVolumeChange = { viewModel.adjustVolume(it) },
                onMuteToggle = { viewModel.toggleMute() },
                onScalingChange = { viewModel.updateVideoScaling(it) },
                onSpeedChange = { viewModel.changePlaybackSpeed(it) }
            )
        }

        // 5. Diagnostics Analytics Stats Panel (Floating Glass Panel)
        AnimatedVisibility(
            visible = showStats,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 90.dp, end = 24.dp)
        ) {
            PlayerDiagnosticsCard(
                stats = playerStats,
                isConnected = networkState.isConnected,
                networkType = networkState.type
            )
        }
    }
}

@Composable
fun PlayerTopBar(
    channelName: String,
    networkType: NetworkType,
    networkConnected: Boolean,
    onSettingsClick: () -> Unit,
    onToggleStats: () -> Unit,
    isStatsActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xE6000000), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left details: Name and Live Status
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF003C))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = channelName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = if (!networkConnected) Icons.Default.CloudOff
                    else when (networkType) {
                        NetworkType.WIFI -> Icons.Default.NetworkWifi
                        NetworkType.ETHERNET -> Icons.Default.Tv
                        else -> Icons.Default.SignalCellularAlt
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (networkConnected) Color(0xFF00FF66) else Color(0xFFFF003C),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (networkConnected) networkType.name else "OFFLINE",
                        color = if (networkConnected) Color.LightGray else Color(0xFFFF003C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Right side: Settings & Diagnostics buttons
        Row {
            var isStatsBtnFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onToggleStats,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isStatsBtnFocused) Color(0x3300F0FF) else Color(0x1A000000))
                    .border(
                        1.dp,
                        if (isStatsBtnFocused) Color(0xFF00F0FF) else Color(0x1AFFFFFF),
                        CircleShape
                    )
                    .onFocusChanged { isStatsBtnFocused = it.isFocused }
                    .testTag("player_stats_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Diagnostics",
                    tint = if (isStatsActive) Color(0xFF00F0FF) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            var isSettingsBtnFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSettingsBtnFocused) Color(0x3300F0FF) else Color(0x1A000000))
                    .border(
                        1.dp,
                        if (isSettingsBtnFocused) Color(0xFF00F0FF) else Color(0x1AFFFFFF),
                        CircleShape
                    )
                    .onFocusChanged { isSettingsBtnFocused = it.isFocused }
                    .testTag("player_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PlayerBufferingOverlay(bufferPercentage: Int) {
    Surface(
        color = Color(0x99000000),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00F0FF),
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "BUFFERING",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$bufferPercentage% cached",
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PlayerErrorOverlay(
    errorMessage: String,
    reconnectAttempts: Int,
    onRetry: () -> Unit
) {
    Surface(
        color = Color(0xE60A0D14),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, Color(0x33FF003C)),
        modifier = Modifier
            .widthIn(max = 340.dp)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFF003C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF003C),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "STREAM OFFLINE",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = errorMessage,
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF003C),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Retrying Stream (Attempt: $reconnectAttempts)...",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            var isRetryFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = onRetry,
                color = if (isRetryFocused) Color(0xFFFF003C) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isRetryFocused) Color.White else Color(0x26FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .onFocusChanged { isRetryFocused = it.isFocused }
                    .focusable()
                    .testTag("player_error_retry_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "RELOAD NOW",
                        color = if (isRetryFocused) Color.White else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerBottomControls(
    playerState: PlayerState,
    volume: Float,
    isMuted: Boolean,
    playbackSpeed: Float,
    videoScaling: Int,
    isFullscreen: Boolean,
    playButtonFocusRequester: FocusRequester,
    onFullscreenToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onScalingChange: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Surface(
        color = Color(0xD90C121F), // Semi-transparent luxury dark blue-grey background
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Buffer percentage bar or playback indicator placeholder (HLS streams are live so we display dynamic stats)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Block: Play, Pause, Stop, Reload (Quick action buttons)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause Action
                    var isPlayFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .focusRequester(playButtonFocusRequester)
                            .background(if (isPlayFocused) Color(0x2600F0FF) else Color(0x0CFFFFFF))
                            .border(
                                1.dp,
                                if (isPlayFocused) Color(0xFF00F0FF) else Color(0x1AFFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isPlayFocused = it.isFocused }
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playerState == PlayerState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Reload Action
                    var isReloadFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onReload,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isReloadFocused) Color(0x2600F0FF) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (isReloadFocused) Color(0xFF00F0FF) else Color(0x12FFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isReloadFocused = it.isFocused }
                            .testTag("player_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Stop Action
                    var isStopFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isStopFocused) Color(0x2600F0FF) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (isStopFocused) Color(0xFF00F0FF) else Color(0x12FFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isStopFocused = it.isFocused }
                            .testTag("player_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center Block: Adaptive Sound/Audio Volume Slider (Touch and TV friendly)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    var isMuteBtnFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onMuteToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isMuteBtnFocused) Color(0x2600F0FF) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (isMuteBtnFocused) Color(0xFF00F0FF) else Color(0x12FFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isMuteBtnFocused = it.isFocused }
                            .testTag("player_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute/Unmute",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Slider(
                        value = if (isMuted) 0f else volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F0FF),
                            activeTrackColor = Color(0xFF00F0FF),
                            inactiveTrackColor = Color(0x26FFFFFF)
                        )
                    )
                }

                // Right Block: Aspect Ratio Adjuster and Speed Control
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Aspect ratio circular button triggers next mode
                    var isScalingBtnFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            val next = (videoScaling + 1) % 3
                            onScalingChange(next)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isScalingBtnFocused) Color(0x2600F0FF) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (isScalingBtnFocused) Color(0xFF00F0FF) else Color(0x12FFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isScalingBtnFocused = it.isFocused }
                            .testTag("player_aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = when (videoScaling) {
                            1 -> "Fill"
                            2 -> "Stretch"
                            else -> "Fit"
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(44.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Speed Adjuster Action
                    var isSpeedBtnFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            val nextSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            onSpeedChange(nextSpeed)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSpeedBtnFocused) Color(0x2600F0FF) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (isSpeedBtnFocused) Color(0xFF00F0FF) else Color(0x12FFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isSpeedBtnFocused = it.isFocused }
                            .testTag("player_speed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Speed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${playbackSpeed}x",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Fullscreen Toggle Action
                    var isFullscreenBtnFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onFullscreenToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isFullscreenBtnFocused) Color(0x2600F0FF) else Color(0x08FFFFFF))
                            .border(
                                1.dp,
                                if (isFullscreenBtnFocused) Color(0xFF00F0FF) else Color(0x12FFFFFF),
                                CircleShape
                            )
                            .onFocusChanged { isFullscreenBtnFocused = it.isFocused }
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerDiagnosticsCard(
    stats: PlayerStats,
    isConnected: Boolean,
    networkType: NetworkType
) {
    Surface(
        color = Color(0xF2070B12), // High opacity dark glass card
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, Color(0x2600F0FF)),
        modifier = Modifier.width(260.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "STREAM METRICS",
                color = Color(0xFF00F0FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Diagnostic stats row items
            DiagnosticRow(label = "Resolution", value = stats.resolution)
            DiagnosticRow(
                label = "Video Bitrate",
                value = if (stats.bitrate > 0) "${stats.bitrate / 1000} kbps" else "Detecting..."
            )
            DiagnosticRow(
                label = "Frame Rate",
                value = if (stats.frameRate > 0) "${stats.frameRate.toInt()} FPS" else "Detecting..."
            )
            DiagnosticRow(label = "Buffer State", value = "${stats.bufferPercentage}% Cached")
            DiagnosticRow(
                label = "Network Speed",
                value = formatNetworkSpeed(stats.networkSpeedBytesSec)
            )
            DiagnosticRow(label = "Reconnect Count", value = stats.reconnectCounter.toString())
            DiagnosticRow(
                label = "Broadcasting Type",
                value = if (stats.isLive) "HLS Live Stream" else "VOD"
            )
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

fun formatNetworkSpeed(bytesSec: Long): String {
    if (bytesSec <= 0) return "0 KB/s"
    val kb = bytesSec / 1024f
    return if (kb > 1024) {
        String.format("%.2f MB/s", kb / 1024f)
    } else {
        String.format("%.1f KB/s", kb)
    }
}
