package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WhitelistedArtistsViewModel
@Inject
constructor(
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val searchQuery = MutableStateFlow("")

    // Expose sync progress from SyncUtils
    val syncProgress = syncUtils.whitelistSyncProgress

    val allArtists =
        combine(
            database.allWhitelistedArtistsByName(),
            searchQuery
        ) { artists, query ->
            Timber.d("WhitelistedArtistsVM: Total whitelisted artists from DB: ${artists.size}, Search query: '$query'")
            artists.forEach { artist ->
                Timber.d("WhitelistedArtistsVM: Artist in DB: '${artist.artist.name}' (ID: ${artist.id})")
            }
            val filtered = if (query.isBlank()) {
                artists
            } else {
                artists.filter { artist ->
                    val matches = artist.artist.name.contains(query, ignoreCase = true)
                    Timber.d("WhitelistedArtistsVM: Artist '${artist.artist.name}' matches query '$query': $matches")
                    matches
                }
            }
            Timber.d("WhitelistedArtistsVM: Filtered result: ${filtered.size} artists")
            filtered
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.syncArtistWhitelist()  // Fixed: was calling syncArtistsSubscriptions() by mistake
        }
    }
}
