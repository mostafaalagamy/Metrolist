package com.metrolist.sync

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.common.constants.IS_SYNC_ENABLED
import com.metrolist.common.data.DataStoreUtil
import com.metrolist.common.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveredDevice(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
    val isSelf: Boolean = false
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val serviceDiscoverer: ServiceDiscoverer
) : ViewModel() {
    private val userEmail: StateFlow<String?> = dataStoreUtil.getEmail()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    init {
        viewModelScope.launch {
            dataStoreUtil.isSyncEnabled().collect { isSyncEnabled ->
                if (isSyncEnabled) {
                    startDiscovery()
                } else {
                    serviceDiscoverer.stopDiscovery()
                }
            }
        }
    }

    private fun startDiscovery() {
        serviceDiscoverer.startDiscovery(
            onServiceResolved = { serviceInfo ->
                viewModelScope.launch {
                    val discoveredDevice = DiscoveredDevice(
                        serviceName = serviceInfo.serviceName,
                        hostAddress = serviceInfo.host.hostAddress,
                        port = serviceInfo.port,
                        isSelf = isSelfDevice(serviceInfo)
                    )
                    _discoveredDevices.value = _discoveredDevices.value + discoveredDevice
                }
            },
            onServiceLost = { serviceInfo ->
                _discoveredDevices.value = _discoveredDevices.value.filter {
                    it.serviceName != serviceInfo.serviceName
                }
            }
        )
    }

    private suspend fun isSelfDevice(serviceInfo: NsdServiceInfo): Boolean {
        val deviceEmail = serviceInfo.attributes["email"]?.toString(Charsets.UTF_8)
        return userEmail.first() == deviceEmail
    }

    override fun onCleared() {
        super.onCleared()
        serviceDiscoverer.stopDiscovery()
    }
}
