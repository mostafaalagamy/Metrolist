package com.metrolist.music.di

import android.content.Context
import com.metrolist.music.constants.EnableAutomaticOfflineModeKey
import com.metrolist.music.constants.ForceOfflineModeKey
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineStateRepository @Inject constructor(
    @ApplicationContext context: Context,
    networkConnectivityObserver: NetworkConnectivityObserver
) {
    val isOffline: StateFlow<Boolean> = combine(
        context.dataStore.data.map { it[EnableAutomaticOfflineModeKey] ?: true },
        context.dataStore.data.map { it[ForceOfflineModeKey] ?: false },
        networkConnectivityObserver.networkStatus
    ) { enableAutomatic, forceOffline, isConnected ->
        forceOffline || (enableAutomatic && !isConnected)
    }.stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(5000), false)
}
