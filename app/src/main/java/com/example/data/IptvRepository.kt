package com.example.data

import com.example.PlayerConfig

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val isLive: Boolean = true,
    val logoUrl: String? = null
)

interface IptvRepository {
    fun getActiveChannel(): Channel
}

class IptvRepositoryImpl : IptvRepository {
    override fun getActiveChannel(): Channel {
        // Returns the single configured stream; easy to expand to local or remote playlists later.
        return Channel(
            id = "easy_iptv_live",
            name = "Live Stream",
            streamUrl = PlayerConfig.STREAM_URL,
            isLive = true
        )
    }
}
