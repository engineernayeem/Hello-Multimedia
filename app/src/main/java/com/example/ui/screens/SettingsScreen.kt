package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PlayerConfig
import com.example.ui.viewmodel.IptvViewModel

@Composable
fun SettingsScreen(
    viewModel: IptvViewModel,
    onBack: () -> Unit
) {
    val autoPlay by viewModel.autoPlay.collectAsState()
    val keepAwake by viewModel.keepScreenAwake.collectAsState()
    val pipEnabled by viewModel.pipEnabled.collectAsState()
    val retryInterval by viewModel.retryInterval.collectAsState()
    val videoScaling by viewModel.videoScaling.collectAsState()
    val backgroundAudio by viewModel.backgroundAudio.collectAsState()

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onBack)
        },
        containerColor = Color(0xFF070B12) // Rich midnight background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF070B12), Color(0xFF0C121F))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp) // Optimized for foldables and tablets
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Player Experience",
                    color = Color(0xFF00F0FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Auto Play Setting Row
                SettingsToggleRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Auto Play Stream",
                    subtitle = "Automatically resume the live HLS stream on startup",
                    checked = autoPlay,
                    onCheckedChange = { viewModel.updateAutoPlay(it) },
                    testTag = "setting_autoplay_switch"
                )

                // 2. Keep Screen Awake Setting Row
                SettingsToggleRow(
                    icon = Icons.Default.Tv,
                    title = "Keep Screen Awake",
                    subtitle = "Prevent the device screen from sleeping during active streaming",
                    checked = keepAwake,
                    onCheckedChange = { viewModel.updateKeepScreenAwake(it) },
                    testTag = "setting_keep_awake_switch"
                )

                // 3. Picture in Picture Setting Row
                SettingsToggleRow(
                    icon = Icons.Default.Visibility,
                    title = "Picture-in-Picture",
                    subtitle = "Automatically trigger PiP window on pressing home",
                    checked = pipEnabled,
                    onCheckedChange = { viewModel.updatePipEnabled(it) },
                    testTag = "setting_pip_switch"
                )

                // 4. Background Audio Setting Row
                SettingsToggleRow(
                    icon = Icons.Default.MusicNote,
                    title = "Background Playback",
                    subtitle = "Continue playing audio stream when app is minimized",
                    checked = backgroundAudio,
                    onCheckedChange = { viewModel.updateBackgroundAudio(it) },
                    testTag = "setting_bg_audio_switch"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Buffer & Scaling",
                    color = Color(0xFF00F0FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 5. Video Scaling Multi-choice Row
                SettingsScalingRow(
                    currentScaling = videoScaling,
                    onScalingChange = { viewModel.updateVideoScaling(it) }
                )

                // 6. Retry Connection Interval Row
                SettingsRetryIntervalRow(
                    currentInterval = retryInterval,
                    onIntervalChange = { viewModel.updateRetryInterval(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Application Info",
                    color = Color(0xFF00F0FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // App version and metadata
                AppInfoCard()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "About Developer",
                    color = Color(0xFF00F0FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AboutDeveloperCard()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("settings_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Player",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "SETTINGS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "Customize live player preferences",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0x2600F0FF) else Color(0x0CFFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isFocused) Color(0xFF00F0FF) else Color(0x1AFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x12FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isFocused) Color(0xFF00F0FF) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF00F0FF),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0x33FFFFFF)
                )
            )
        }
    }
}

@Composable
fun SettingsScalingRow(
    currentScaling: Int,
    onScalingChange: (Int) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val options = listOf("Fit Screen", "Fill Screen", "Stretch")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0x2600F0FF) else Color(0x0CFFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isFocused) Color(0xFF00F0FF) else Color(0x1AFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x12FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (isFocused) Color(0xFF00F0FF) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Preferred Aspect Ratio Scaling",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select default layout formatting for HLS streams",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-segment toggle button selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, title ->
                    val isSelected = currentScaling == index
                    var isBtnFocused by remember { mutableStateOf(false) }

                    Button(
                        onClick = { onScalingChange(index) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF00F0FF) else if (isBtnFocused) Color(0x33FFFFFF) else Color(0x14FFFFFF),
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .onFocusChanged { isBtnFocused = it.isFocused }
                            .border(
                                width = 1.dp,
                                color = if (isBtnFocused) Color(0xFF00F0FF) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("scaling_btn_$index"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRetryIntervalRow(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val intervals = listOf(1, 3, 5, 10)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0x2600F0FF) else Color(0x0CFFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isFocused) Color(0xFF00F0FF) else Color(0x1AFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x12FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (isFocused) Color(0xFF00F0FF) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Connection Auto-Retry Interval",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pause duration between connection attempts on fail",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector intervals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                intervals.forEach { seconds ->
                    val isSelected = currentInterval == seconds
                    var isBtnFocused by remember { mutableStateOf(false) }

                    Button(
                        onClick = { onIntervalChange(seconds) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF00F0FF) else if (isBtnFocused) Color(0x33FFFFFF) else Color(0x14FFFFFF),
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .onFocusChanged { isBtnFocused = it.isFocused }
                            .border(
                                width = 1.dp,
                                color = if (isBtnFocused) Color(0xFF00F0FF) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("retry_btn_$seconds"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "${seconds}s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0x06FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0A00F0FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Hello Multimedia",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Optimized ExoPlayer Media3 Client",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version: ${PlayerConfig.APP_VERSION}",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Build: Active",
                        color = Color(0xFF00FF66), // Glowing green build indicator
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AboutDeveloperCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0x06FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFD700)), // Warm gold/yellow tint from logo colors
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Developer",
                        tint = Color(0xFFFFD700), // LogoYellow
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Nayeem Mallik",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lead Developer & Designer",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = Color(0xFF0084FF), // LogoBlue
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "01713873708",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


