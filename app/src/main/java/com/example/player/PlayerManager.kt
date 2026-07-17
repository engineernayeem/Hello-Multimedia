package com.example.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.datasource.DefaultHttpDataSource
import com.example.PlayerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val onPlayerError: (String) -> Unit
) {
    private val TAG = "PlayerManager"
    
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val bandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(context)
    
    // UI state flows from player states
    private val _playbackState = MutableStateFlow(PlayerState.IDLE)
    val playbackState = _playbackState.asStateFlow()

    private val _stats = MutableStateFlow(PlayerStats())
    val stats = _stats.asStateFlow()

    private var retryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Configurable scaling and settings
    private var keepScreenAwakeListener: ((Boolean) -> Unit)? = null
    private var retryIntervalSecs = PlayerConfig.DEFAULT_RETRY_INTERVAL_SEC

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_IDLE -> {
                    _playbackState.value = PlayerState.IDLE
                    keepScreenAwakeListener?.invoke(false)
                }
                Player.STATE_BUFFERING -> {
                    _playbackState.value = PlayerState.BUFFERING
                    keepScreenAwakeListener?.invoke(true)
                }
                Player.STATE_READY -> {
                    _playbackState.value = if (exoPlayer?.playWhenReady == true) PlayerState.PLAYING else PlayerState.PAUSED
                    keepScreenAwakeListener?.invoke(exoPlayer?.playWhenReady == true)
                    
                    // Reset retry counts on successful play
                    cancelRetry()
                }
                Player.STATE_ENDED -> {
                    _playbackState.value = PlayerState.ENDED
                    keepScreenAwakeListener?.invoke(false)
                }
            }
            updateStats()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (_playbackState.value != PlayerState.BUFFERING) {
                _playbackState.value = if (isPlaying) PlayerState.PLAYING else PlayerState.PAUSED
            }
            keepScreenAwakeListener?.invoke(isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer Error: ${error.errorCodeName} (${error.errorCode})", error)
            _playbackState.value = PlayerState.ERROR
            keepScreenAwakeListener?.invoke(false)
            
            val friendlyError = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No Internet Connection"
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "Server Offline or Invalid Stream URL"
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Stream Not Found (404)"
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "HTTP streams not permitted. HTTPS required."
                PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "Live stream buffer behind windows (Catching up)"
                PlaybackException.ERROR_CODE_TIMEOUT -> "Network Timeout"
                else -> "Playback error: ${error.localizedMessage ?: "Unknown Error"}"
            }
            onPlayerError(friendlyError)
            scheduleAutoRetry()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _stats.update {
                it.copy(
                    resolution = "${videoSize.width}x${videoSize.height}",
                    aspectRatio = if (videoSize.height > 0) videoSize.width.toFloat() / videoSize.height else 1.77f
                )
            }
        }
    }

    init {
        initializePlayer()
    }

    fun setKeepScreenAwakeListener(listener: (Boolean) -> Unit) {
        this.keepScreenAwakeListener = listener
    }

    fun setRetryInterval(seconds: Int) {
        this.retryIntervalSecs = seconds
    }

    fun getPlayer(): Player? = exoPlayer

    private fun initializePlayer() {
        if (exoPlayer != null) return

        // 1. Hardware Decoding configuration via RenderersFactory
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
        }

        // 2. Track Selector for Automatic Quality Switching
        trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().build())
        }

        // 3. Custom LoadControl for Buffer Optimization & Fast Startup
        // Optimized for high stability and smooth streaming of IPTV feeds
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                20000, // minBufferMs (increased to 20s to weather network jitter)
                60000, // maxBufferMs (max buffer size 60s)
                2500,  // bufferForPlaybackMs (buffer required to start; 2.5s is safe for stable start)
                5000   // bufferForPlaybackAfterRebufferMs (buffer required to resume after rebuffering; 5s prevents stutter loops)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 4. Build Player
        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .build()
            .apply {
                // Audio Focus
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                setAudioAttributes(audioAttributes, true)

                // Wake Lock / Network Lock
                setWakeMode(C.WAKE_MODE_NETWORK)
                
                // Keep screen awake while playing
                playWhenReady = true
                
                addListener(playerListener)
            }

        Log.d(TAG, "ExoPlayer initialized with hardware decoding, low startup buffer, and audio focus.")
        startStatsTracker()
    }

    fun playStream(url: String) {
        val player = exoPlayer ?: return
        
        // 5. Create Live Optimizations configuration in MediaItem with stable target offset
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.10f)
                    .setMinPlaybackSpeed(0.90f)
                    .setTargetOffsetMs(15000) // 15 seconds target offset to allow healthy caching of chunks
                    .build()
            )
            .build()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)

        // HLS MediaSource with retry capabilities
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        player.setMediaSource(hlsMediaSource)
        player.prepare()
        player.playWhenReady = true
        Log.d(TAG, "Streaming initialized for URL: $url")
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
        cancelRetry()
    }

    fun reload() {
        Log.d(TAG, "Manually reloading stream...")
        cancelRetry()
        _stats.update { it.copy(reconnectCounter = it.reconnectCounter + 1) }
        val currentChannel = PlayerConfig.STREAM_URL
        playStream(currentChannel)
    }

    fun setMute(mute: Boolean) {
        exoPlayer?.volume = if (mute) 0f else 1f
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }

    private fun scheduleAutoRetry() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            _stats.update { it.copy(reconnectCounter = it.reconnectCounter + 1) }
            Log.d(TAG, "Stream failed. Retrying in $retryIntervalSecs seconds... Count: ${_stats.value.reconnectCounter}")
            delay(retryIntervalSecs * 1000L)
            reload()
        }
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
    }

    private fun updateStats() {
        val player = exoPlayer ?: return
        val currentFormat = player.videoFormat
        
        _stats.update { currentStats ->
            currentStats.copy(
                bufferPercentage = player.bufferedPercentage,
                networkSpeedBytesSec = bandwidthMeter.bitrateEstimate / 8, // Convert bits/sec to bytes/sec
                bitrate = currentFormat?.bitrate ?: 0,
                frameRate = currentFormat?.frameRate ?: 0f,
                isLive = player.isCurrentMediaItemLive
            )
        }
    }

    private var statsTrackerJob: Job? = null
    private fun startStatsTracker() {
        statsTrackerJob?.cancel()
        statsTrackerJob = scope.launch {
            while (true) {
                updateStats()
                delay(1000) // Update stats every second
            }
        }
    }

    fun release() {
        statsTrackerJob?.cancel()
        cancelRetry()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        Log.d(TAG, "Player released.")
    }
}

enum class PlayerState {
    IDLE, PLAYING, PAUSED, BUFFERING, ENDED, ERROR
}

data class PlayerStats(
    val resolution: String = "Unknown",
    val bitrate: Int = 0,
    val frameRate: Float = 0f,
    val bufferPercentage: Int = 0,
    val isLive: Boolean = false,
    val networkSpeedBytesSec: Long = 0,
    val reconnectCounter: Int = 0,
    val aspectRatio: Float = 1.77f // 16:9 default
)
