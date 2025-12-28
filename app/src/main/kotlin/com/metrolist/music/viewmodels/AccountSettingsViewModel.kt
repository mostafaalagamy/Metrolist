/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.App
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val syncUtils: SyncUtils,
) : ViewModel() {

    /**
     * Logout user and clear all synced content to prevent data mixing between accounts
     */
    fun logoutAndClearSyncedContent(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all YouTube Music synced content first
            syncUtils.clearAllSyncedContent()
            
            // Then clear account preferences
            App.forgetAccount(context)
            
            // Clear cookie in UI
            onCookieChange("")
        }
    }
}
