package com.metrolist.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerService(port: Int, userEmail: String) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Metrolist Sync"
            serviceType = "_metrolist-sync._tcp"
            setPort(port)
            setAttribute("email", userEmail)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Timber.d("Service registered: %s", serviceInfo.serviceName)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("Service registration failed. Error code: %d", errorCode)
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Timber.d("Service unregistered: %s", serviceInfo.serviceName)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("Service unregistration failed. Error code: %d", errorCode)
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterService() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
            registrationListener = null
        }
    }
}
