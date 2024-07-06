package com.malopieds.innertune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.PlaylistItem
import com.malopieds.innertune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel
    @Inject
    constructor() : ViewModel() {
        val playlists = MutableStateFlow<List<PlaylistItem>?>(null)

        init {
            viewModelScope.launch {
                YouTube
                    .likedPlaylists()
                    .onSuccess {
                        playlists.value = it
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
