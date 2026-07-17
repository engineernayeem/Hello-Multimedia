package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PlayerConfig
import com.example.data.Channel
import com.example.data.IptvRepository
import com.example.data.IptvRepositoryImpl
import com.example.player.PlayerManager
import com.example.player.PlayerState
import com.example.player.PlayerStats
import com.example.utils.NetworkMonitor
import com.example.utils.NetworkState
import com.example.utils.NetworkType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "IptvViewModel"

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        PlayerConfig.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val repository: IptvRepository = IptvRepositoryImpl()
    private val networkMonitor = NetworkMonitor(application)

    // Routing State
    private val _currentScreen = MutableStateFlow<ScreenState>(ScreenState.Splash)
    val currentScreen = _currentScreen.asStateFlow()

    // Player State
    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    val playerState = _playerState.asStateFlow()

    private val _playerStats = MutableStateFlow(PlayerStats())
    val playerStats = _playerStats.asStateFlow()

    // Network State
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState = _networkState.asStateFlow()

    // Active Channel State
    private val _currentChannel = MutableStateFlow(repository.getActiveChannel())
    val currentChannel = _currentChannel.asStateFlow()

    // Error Notifications State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Volume and controls
    private val _volume = MutableStateFlow(1f)
    val volume = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    // Settings States (backed by SharedPreferences)
    private val _autoPlay = MutableStateFlow(PlayerConfig.DEFAULT_AUTO_PLAY)
    val autoPlay = _autoPlay.asStateFlow()

    private val _keepScreenAwake = MutableStateFlow(PlayerConfig.DEFAULT_KEEP_AWAKE)
    val keepScreenAwake = _keepScreenAwake.asStateFlow()

    private val _pipEnabled = MutableStateFlow(PlayerConfig.DEFAULT_PIP_ENABLED)
    val pipEnabled = _pipEnabled.asStateFlow()

    private val _retryInterval = MutableStateFlow(PlayerConfig.DEFAULT_RETRY_INTERVAL_SEC)
    val retryInterval = _retryInterval.asStateFlow()

    private val _videoScaling = MutableStateFlow(PlayerConfig.DEFAULT_VIDEO_SCALING)
    val videoScaling = _videoScaling.asStateFlow()

    private val _backgroundAudio = MutableStateFlow(PlayerConfig.DEFAULT_BACKGROUND_AUDIO)
    val backgroundAudio = _backgroundAudio.asStateFlow()

    private var playerManager: PlayerManager? = null
    private var isScreenAwakeListener: ((Boolean) -> Unit)? = null
    private var networkMonitorJob: Job? = null
    private var playerStatsJob: Job? = null

    init {
        loadSettings()
        observeNetwork()
        startSplashScreenTimer()
    }

    private fun loadSettings() {
        _autoPlay.value = sharedPrefs.getBoolean(PlayerConfig.KEY_AUTO_PLAY, PlayerConfig.DEFAULT_AUTO_PLAY)
        _keepScreenAwake.value = sharedPrefs.getBoolean(PlayerConfig.KEY_KEEP_AWAKE, PlayerConfig.DEFAULT_KEEP_AWAKE)
        _pipEnabled.value = sharedPrefs.getBoolean(PlayerConfig.KEY_PIP_ENABLED, PlayerConfig.DEFAULT_PIP_ENABLED)
        _retryInterval.value = sharedPrefs.getInt(PlayerConfig.KEY_RETRY_INTERVAL, PlayerConfig.DEFAULT_RETRY_INTERVAL_SEC)
        _videoScaling.value = sharedPrefs.getInt(PlayerConfig.KEY_VIDEO_SCALING, PlayerConfig.DEFAULT_VIDEO_SCALING)
        _backgroundAudio.value = sharedPrefs.getBoolean(PlayerConfig.KEY_BACKGROUND_AUDIO, PlayerConfig.DEFAULT_BACKGROUND_AUDIO)
    }

    private fun startSplashScreenTimer() {
        viewModelScope.launch {
            // Give 3.5 seconds for a premium animated splash experience
            delay(3500)
            _currentScreen.value = ScreenState.Player
            if (_autoPlay.value) {
                initializeAndPlay()
            }
        }
    }

    private fun observeNetwork() {
        networkMonitorJob?.cancel()
        networkMonitorJob = viewModelScope.launch {
            var wasOffline = false
            networkMonitor.connectivityFlow.collectLatest { state ->
                _networkState.value = state
                if (state.isConnected) {
                    Log.d(TAG, "Network available. Type: ${state.type}")
                    if (wasOffline) {
                        _errorMessage.value = null
                        // Re-initialize or reload stream on connection restoration
                        playerManager?.reload()
                        wasOffline = false
                    }
                } else {
                    Log.d(TAG, "Network connection lost!")
                    _errorMessage.value = "No Internet Connection"
                    wasOffline = true
                }
            }
        }
    }

    fun setKeepScreenAwakeCallback(listener: (Boolean) -> Unit) {
        this.isScreenAwakeListener = listener
        playerManager?.setKeepScreenAwakeListener { shouldAwake ->
            if (_keepScreenAwake.value) {
                listener(shouldAwake)
            } else {
                listener(false)
            }
        }
    }

    fun initializeAndPlay() {
        if (playerManager == null) {
            playerManager = PlayerManager(getApplication()) { error ->
                _errorMessage.value = error
            }
            playerManager?.setRetryInterval(_retryInterval.value)
            
            // Connect Awake listeners
            isScreenAwakeListener?.let { setKeepScreenAwakeCallback(it) }

            // Observe player manager updates
            playerStatsJob?.cancel()
            playerStatsJob = viewModelScope.launch {
                launch {
                    playerManager?.playbackState?.collectLatest { state ->
                        _playerState.value = state
                        if (state == PlayerState.PLAYING) {
                            _errorMessage.value = null
                        }
                    }
                }
                launch {
                    playerManager?.stats?.collectLatest { stats ->
                        _playerStats.value = stats
                    }
                }
            }
        }

        val channel = _currentChannel.value
        _errorMessage.value = null
        playerManager?.playStream(channel.streamUrl)
    }

    fun togglePlayPause() {
        playerManager?.togglePlayPause()
    }

    fun reloadStream() {
        _errorMessage.value = null
        playerManager?.reload()
    }

    fun stopStream() {
        playerManager?.stop()
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        playerManager?.setMute(newMute)
    }

    fun adjustVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        if (_isMuted.value && vol > 0f) {
            _isMuted.value = false
            playerManager?.setMute(false)
        }
        playerManager?.setVolume(_volume.value)
    }

    fun changePlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        playerManager?.setPlaybackSpeed(speed)
    }

    // Setting modifiers
    fun updateAutoPlay(enabled: Boolean) {
        _autoPlay.value = enabled
        sharedPrefs.edit().putBoolean(PlayerConfig.KEY_AUTO_PLAY, enabled).apply()
    }

    fun updateKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        sharedPrefs.edit().putBoolean(PlayerConfig.KEY_KEEP_AWAKE, enabled).apply()
        // If turned off, notify layout to turn off awake locks
        if (!enabled) {
            isScreenAwakeListener?.invoke(false)
        } else {
            val playing = _playerState.value == PlayerState.PLAYING || _playerState.value == PlayerState.BUFFERING
            isScreenAwakeListener?.invoke(playing)
        }
    }

    fun updatePipEnabled(enabled: Boolean) {
        _pipEnabled.value = enabled
        sharedPrefs.edit().putBoolean(PlayerConfig.KEY_PIP_ENABLED, enabled).apply()
    }

    fun updateRetryInterval(seconds: Int) {
        _retryInterval.value = seconds
        sharedPrefs.edit().putInt(PlayerConfig.KEY_RETRY_INTERVAL, seconds).apply()
        playerManager?.setRetryInterval(seconds)
    }

    fun updateVideoScaling(scaling: Int) {
        _videoScaling.value = scaling
        sharedPrefs.edit().putInt(PlayerConfig.KEY_VIDEO_SCALING, scaling).apply()
    }

    fun updateBackgroundAudio(enabled: Boolean) {
        _backgroundAudio.value = enabled
        sharedPrefs.edit().putBoolean(PlayerConfig.KEY_BACKGROUND_AUDIO, enabled).apply()
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun getPlayerInstance(): PlayerManager? = playerManager

    override fun onCleared() {
        super.onCleared()
        playerStatsJob?.cancel()
        networkMonitorJob?.cancel()
        playerManager?.release()
        playerManager = null
    }
}

sealed class ScreenState {
    object Splash : ScreenState()
    object Player : ScreenState()
    object Settings : ScreenState()
}
