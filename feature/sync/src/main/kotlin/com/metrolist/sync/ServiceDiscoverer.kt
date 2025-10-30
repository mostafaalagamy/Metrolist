package com.metrolist.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceDiscoverer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery(
        onServiceResolved: (NsdServiceInfo) -> Unit,
        onServiceLost: (NsdServiceInfo) -> Unit
    ) {
        if (discoveryListener != null) {
            Timber.d("Discovery already running")
            return
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.d("Service found: %s", service)
                resolveService(service, onServiceResolved)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.d("Service lost: %s", service)
                onServiceLost(service)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("Service discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code: %d", errorCode)
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Stop Discovery failed: Error code: %d", errorCode)
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(
            "_metrolist-sync._tcp",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    private fun resolveService(serviceInfo: NsdServiceInfo, onServiceResolved: (NsdServiceInfo) -> Unit) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("Resolve failed: %s, error code: %d", serviceInfo, errorCode)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Timber.d("Service resolved: %s", serviceInfo)
                coroutineScope.launch {
                    onServiceResolved(serviceInfo)
                }
            }
        }
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
    }
}
