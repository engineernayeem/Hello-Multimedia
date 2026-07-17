package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkType {
    WIFI, CELLULAR, ETHERNET, NONE
}

class NetworkMonitor(private val context: Context) {

    val connectivityFlow: Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = connectivityManager.getNetworkCapabilities(network)
                val type = getNetworkType(caps)
                trySend(NetworkState(isConnected = true, type = type))
            }

            override fun onLost(network: Network) {
                // Check if there's any active connection left
                val isStillConnected = checkConnected(connectivityManager)
                val type = if (isStillConnected) getActiveType(connectivityManager) else NetworkType.NONE
                trySend(NetworkState(isConnected = isStillConnected, type = type))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val type = getNetworkType(networkCapabilities)
                trySend(NetworkState(isConnected = true, type = type))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        val initialConnected = checkConnected(connectivityManager)
        val initialType = if (initialConnected) getActiveType(connectivityManager) else NetworkType.NONE
        trySend(NetworkState(isConnected = initialConnected, type = initialType))

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun checkConnected(manager: ConnectivityManager): Boolean {
        val activeNetwork = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getActiveType(manager: ConnectivityManager): NetworkType {
        val activeNetwork = manager.activeNetwork ?: return NetworkType.NONE
        val caps = manager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
        return getNetworkType(caps)
    }

    private fun getNetworkType(caps: NetworkCapabilities?): NetworkType {
        if (caps == null) return NetworkType.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }
}

data class NetworkState(
    val isConnected: Boolean = false,
    val type: NetworkType = NetworkType.NONE
)
