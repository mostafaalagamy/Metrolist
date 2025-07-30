package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    database: MusicDatabase
) : ViewModel() {
    private val playlistId = savedStateHandle.get<String>("playlistId")!!

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()
    
    val dbPlaylist = database.playlistByBrowseId(playlistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    var continuation: String? = null
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            
            YouTube.playlist(playlistId)
                .onSuccess { playlistPage ->
                    playlist.value = playlistPage.playlist
                    playlistSongs.value = playlistPage.songs.distinctBy { it.id }
                    continuation = playlistPage.songsContinuation
                    _isLoading.value = false
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Failed to load playlist"
                    _isLoading.value = false
                    reportException(throwable)
                }
        }
    }

    fun loadMoreSongs() {
        if (_isLoadingMore.value) return // Prevent multiple concurrent requests
        
        continuation?.let {
            viewModelScope.launch(Dispatchers.IO) {
                _isLoadingMore.value = true
                YouTube.playlistContinuation(it)
                    .onSuccess { playlistContinuationPage ->
                        val currentSongs = playlistSongs.value.toMutableList()
                        currentSongs.addAll(playlistContinuationPage.songs)
                        playlistSongs.value = currentSongs.distinctBy { it.id }
                        continuation = playlistContinuationPage.continuation
                        _isLoadingMore.value = false
                    }.onFailure { throwable ->
                        _isLoadingMore.value = false
                        reportException(throwable)
                    }
            }
        }
    }

    fun retry() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            
            YouTube.playlist(playlistId)
                .onSuccess { playlistPage ->
                    playlist.value = playlistPage.playlist
                    playlistSongs.value = playlistPage.songs.distinctBy { it.id }
                    continuation = playlistPage.songsContinuation
                    _isLoading.value = false
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Failed to load playlist"
                    _isLoading.value = false
                    reportException(throwable)
                }
        }
    }
}
