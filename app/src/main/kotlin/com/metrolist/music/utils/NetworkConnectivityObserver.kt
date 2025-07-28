package com.metrolist.music.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class NetworkConnectivityObserver(
    context: Context
) : DefaultLifecycleObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = Channel<Boolean>(Channel.CONFLATED)
    val networkStatus = _networkStatus.receiveAsFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkNetworkStatus()
        override fun onLost(network: Network) = checkNetworkStatus()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = checkNetworkStatus()
    }

    fun checkNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val hasInternet = connectivityManager.getNetworkCapabilities(activeNetwork)?.let { capabilities ->
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
        _networkStatus.trySend(hasInternet)
    }

    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        checkNetworkStatus()
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        register()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        unregister()
    }
}