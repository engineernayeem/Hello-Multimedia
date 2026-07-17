package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PlayerConfig
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0.0f) }

    LaunchedEffect(Unit) {
        // Run entry animations in parallel
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1800)
        )
    }

    // Luxurious Dark-Blue to Black Radial-like Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080C16),
                        Color(0xFF0F1524),
                        Color(0xFF030508)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant glowing card wrapping our generated IPTV logo
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x1F00F0FF)) // Subtle neon border glow
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF141F35), Color(0xFF0D1424))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = "Hello Multimedia Logo",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Application Title
            Text(
                text = "HELLO MULTIMEDIA",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.alpha(alpha.value),
                textAlign = TextAlign.Center
            )

            Text(
                text = "PLAYER",
                color = Color(0xFF00F0FF), // Neon Cyan accent
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alpha(alpha.value),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Glowing glass progress indicator
            CircularProgressIndicator(
                color = Color(0xFF00F0FF),
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(36.dp)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preparing Stream...",
                color = Color.Gray,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha.value)
            )
        }

        // App version footer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(0.5f)
            ) {
                Text(
                    text = "v${PlayerConfig.APP_VERSION}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
