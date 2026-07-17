package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.player.PlayerState
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IptvViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val viewModel: IptvViewModel by viewModels()
    private var isPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge full bleeding layout
        enableEdgeToEdge()

        // Bind Screen Awake changes to android Window Flags
        viewModel.setKeepScreenAwakeCallback { shouldKeepAwake ->
            runOnUiThread {
                if (shouldKeepAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    Log.d(TAG, "Screen Awake Lock: ACTIVE")
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    Log.d(TAG, "Screen Awake Lock: INACTIVE")
                }
            }
        }

        setContent {
            MyApplicationTheme(darkTheme = true) { // Explicitly Dark Theme for luxury IPTV vibe
                val currentScreen by viewModel.currentScreen.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Ignore padding on immersive streaming player to leverage absolute full-bleed screen
                    when (currentScreen) {
                        is ScreenState.Splash -> {
                            SplashScreen()
                        }
                        is ScreenState.Player -> {
                            PlayerScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { viewModel.navigateTo(ScreenState.Settings) },
                                toggleImmersive = { hideSystemBars -> toggleImmersiveMode(hideSystemBars) }
                            )
                        }
                        is ScreenState.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(ScreenState.Player) }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Immersive Full-Screen mode controller.
     * Hides/shows the system Status bar and Navigation gesture pill on demand.
     */
    private fun toggleImmersiveMode(hide: Boolean) {
        if (isPipMode) return // Skip modifying window decorations inside PiP
        runOnUiThread {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hide) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    /**
     * Standard TV Remote controller and hardware media key event handlers.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept standard TV media action clicks when Player is active
        if (viewModel.currentScreen.value is ScreenState.Player) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    viewModel.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    viewModel.stopStream()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    // Standard TV back button navigates or exits gracefully
                    if (viewModel.currentScreen.value !is ScreenState.Player) {
                        viewModel.navigateTo(ScreenState.Player)
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Picture-in-Picture automatic triggering when minimization occurs
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Enter Picture-In-Picture mode if settings permit and stream is active
        val pipEnabled = viewModel.pipEnabled.value
        val isStreaming = viewModel.playerState.value == PlayerState.PLAYING || 
                          viewModel.playerState.value == PlayerState.BUFFERING

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipEnabled && isStreaming) {
            try {
                val params = PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
                Log.d(TAG, "Entering Picture-In-Picture Mode successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed entering Picture-in-Picture: ${e.message}")
            }
        }
    }

    /**
     * Listen to OS picture in picture status changes and hide controls accordingly
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // Minimize audio context and ensure zero controls overlay shown inside tiny PiP frame
            toggleImmersiveMode(true)
        } else {
            // Restore normal player screen
            toggleImmersiveMode(false)
        }
    }

    override fun onStop() {
        super.onStop()
        // If the activity is finishing (e.g. PiP closed or app exited), or if background playback setting is disabled, stop the player automatically
        if (isFinishing || (!viewModel.backgroundAudio.value && !isPipMode)) {
            viewModel.stopStream()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure stream is stopped on activity destruction to release wake locks and resources safely
        viewModel.stopStream()
    }
}
