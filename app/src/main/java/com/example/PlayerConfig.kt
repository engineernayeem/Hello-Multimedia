package com.example

import android.net.Uri

object PlayerConfig {
    // CHANGE THIS URL to your IPTV server stream (.m3u8)
    const val STREAM_URL = "http://10.101.101.101:8080/live/channel3/index.m3u8"
    
    // Alternative free live stream examples:
    // "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
    // "https://test-streams.mux.dev/x36xhqq/x36xhqq.m3u8"

    const val APP_VERSION = "1.0.0-Beta"
    
    // SharedPreferences Keys
    const val PREFS_NAME = "easy_iptv_prefs"
    const val KEY_AUTO_PLAY = "pref_auto_play"
    const val KEY_KEEP_AWAKE = "pref_keep_awake"
    const val KEY_PIP_ENABLED = "pref_pip_enabled"
    const val KEY_RETRY_INTERVAL = "pref_retry_interval"
    const val KEY_VIDEO_SCALING = "pref_video_scaling"
    const val KEY_BACKGROUND_AUDIO = "pref_background_audio"

    // Default Configuration Values
    const val DEFAULT_AUTO_PLAY = true
    const val DEFAULT_KEEP_AWAKE = true
    const val DEFAULT_PIP_ENABLED = true
    const val DEFAULT_RETRY_INTERVAL_SEC = 3
    const val DEFAULT_BACKGROUND_AUDIO = false
    const val DEFAULT_VIDEO_SCALING = 0 // 0 = Fit, 1 = Fill, 2 = Stretch
}
