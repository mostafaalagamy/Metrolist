package com.metrolist.music.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.ArtistPage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterExplicitAlbums
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.filterWhitelisted
import com.metrolist.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId).map { albums ->
                timber.log.Timber.d("ArtistViewModel: artistId=$artistId, albums from query=${albums.size}, hideExplicit=$hideExplicit")
                albums.forEach { album ->
                    timber.log.Timber.d("ArtistViewModel: album=${album.album.title}, explicit=${album.album.explicit}")
                }
                albums.filterExplicitAlbums(hideExplicit)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map { it[HideExplicitKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    timber.log.Timber.d("ArtistViewModel: Fetched YouTube page with ${page.sections.size} sections")
                    page.sections.forEach { section ->
                        timber.log.Timber.d("ArtistViewModel: Section '${section.title}' has ${section.items.size} items")
                    }

                    val filteredSections = page.sections
                        .filterNot { section ->
                            // Filter "Fans might also like" / similar artist sections
                            val browseId = section.moreEndpoint?.browseId ?: ""
                            val hasSimilarBrowseId = browseId.startsWith("MPLAUC") ||
                                                    browseId.startsWith("MPLART") ||
                                                    browseId.startsWith("MPLREL")

                            // Check for title keywords
                            val hasSimilarTitle = section.title.contains("fans", ignoreCase = true) ||
                                                 section.title.contains("similar", ignoreCase = true) ||
                                                 section.title.contains("also like", ignoreCase = true) ||
                                                 section.title.contains("you might", ignoreCase = true) ||
                                                 section.title.contains("from your library", ignoreCase = true)

                            // Check if section contains only artist recommendations
                            val isOnlyArtists = section.items.isNotEmpty() &&
                                               section.items.all { it is com.metrolist.innertube.models.ArtistItem }

                            // Filter if it matches browseId OR (has similar title AND is only artists)
                            hasSimilarBrowseId || (hasSimilarTitle && isOnlyArtists)
                        }
                        .filterNot { section ->
                            // Filter Videos sections
                            section.title.contains("video", ignoreCase = true) ||
                            section.title.contains("vidéo", ignoreCase = true) || // French
                            section.title.contains("vídeo", ignoreCase = true)    // Spanish/Portuguese
                        }
                        .filterNot { section ->
                            // Filter "Playlists by [Artist]" sections
                            section.items.any { it is com.metrolist.innertube.models.PlaylistItem } &&
                            (section.title.contains("playlist", ignoreCase = true) ||
                             section.title.contains("liste", ignoreCase = true))  // French variations
                        }
                        .map { section ->
                            val originalCount = section.items.size
                            // Only filter explicit content, not by whitelist (we're already on a whitelisted artist's page)
                            val filtered = section.items.filterExplicit(hideExplicit)
                            timber.log.Timber.d("ArtistViewModel: Section '${section.title}': ${originalCount} items -> ${filtered.size} after filtering")
                            section.copy(items = filtered)
                        }

                    artistPage = page.copy(sections = filteredSections)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
