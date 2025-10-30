package com.metrolist.sync.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.sync.DiscoveredDevice
import com.metrolist.sync.SyncViewModel

@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel()
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    DiscoveredDevicesList(devices = discoveredDevices)
}

@Composable
fun DiscoveredDevicesList(devices: List<DiscoveredDevice>) {
    LazyColumn {
        items(devices) { device ->
            DeviceListItem(device = device)
        }
    }
}

@Composable
fun DeviceListItem(device: DiscoveredDevice) {
    Column {
        val deviceName = if (device.isSelf) {
            "${device.serviceName} (This device)"
        } else {
            device.serviceName
        }
        Text(text = deviceName)
    }
}
