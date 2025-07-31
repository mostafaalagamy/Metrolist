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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterExplicitAlbums
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var hasError by mutableStateOf(false)
        private set
    
    private var fetchJob: Job? = null
    
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
            database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page initially
        fetchArtistsFromYTM()
        
        // Reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map { it[HideExplicitKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    // Add a small delay to prevent rapid successive calls
                    delay(100)
                    fetchArtistsFromYTM()
                }
        }
    }

    fun fetchArtistsFromYTM() {
        // Cancel any existing fetch job to prevent race conditions
        fetchJob?.cancel()
        
        fetchJob = viewModelScope.launch {
            try {
                isLoading = true
                hasError = false
                
                // Resolve the correct artist ID for YouTube API
                val youtubeArtistId = resolveYouTubeArtistId(artistId)
                
                // Skip YouTube API call if this is a purely local artist with no YouTube equivalent
                if (youtubeArtistId == null) {
                    artistPage = null
                    hasError = false
                    isLoading = false
                    return@launch
                }
                
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                
                YouTube.artist(youtubeArtistId)
                    .onSuccess { page ->
                        val filteredSections = page.sections
                            .filterNot { section ->
                                section.title.equals("From your library", ignoreCase = true)
                            }
                            .map { section ->
                                section.copy(items = section.items.filterExplicit(hideExplicit))
                            }

                        artistPage = page.copy(sections = filteredSections)
                        hasError = false
                    }.onFailure { throwable ->
                        // If API call fails for a local artist, don't treat it as an error
                        if (artistId.startsWith("LA")) {
                            artistPage = null
                            hasError = false
                        } else {
                            hasError = true
                            artistPage = null
                            reportException(throwable)
                        }
                    }
            } catch (e: Exception) {
                // If it's a local artist, don't treat it as an error
                if (artistId.startsWith("LA")) {
                    artistPage = null
                    hasError = false
                } else {
                    hasError = true
                    artistPage = null
                    reportException(e)
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Resolves the YouTube artist ID from a local artist ID if possible
     * Returns null if this is a purely local artist with no YouTube equivalent
     */
    private suspend fun resolveYouTubeArtistId(inputArtistId: String): String? {
        return when {
            // If it's already a YouTube artist ID, use it directly
            inputArtistId.startsWith("UC") || inputArtistId.startsWith("FEmusic_library_privately_owned_artist") -> {
                inputArtistId
            }
            // If it's a local artist ID, try to find the YouTube equivalent
            inputArtistId.startsWith("LA") -> {
                try {
                    val localArtist = database.artist(inputArtistId).first()
                    
                    // If the local artist has a channelId, use it
                    localArtist?.channelId?.let { channelId ->
                        return channelId
                    }
                    
                    // Try to find a YouTube artist with the same name
                    localArtist?.name?.let { artistName ->
                        val youtubeArtist = database.artistByName(artistName)
                        youtubeArtist?.takeIf { 
                            it.id.startsWith("UC") || it.id.startsWith("FEmusic_library_privately_owned_artist")
                        }?.id
                    }
                } catch (e: Exception) {
                    null
                }
            }
            // Unknown format, try as is
            else -> inputArtistId
        }
    }
    
    fun retry() {
        fetchArtistsFromYTM()
    }
}
