package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.utils.reportException
import com.metrolist.innertube.utils.completedLibraryPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    init {
        viewModelScope.launch {
            YouTube.likedPlaylists().completedLibraryPage()?.onSuccess {
                playlists.value = it.items.filterIsInstance<PlaylistItem>()
            }?.onFailure {
                 reportException(it)
            }
             YouTube.libraryAlbums().completedLibraryPage()?.onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
            }?.onFailure {
                 reportException(it)
             }
             YouTube.libraryArtistsSubscriptions().completedLibraryPage()?.onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>()
            }?.onFailure {
                reportException(it)
             }
        }
    }
}
