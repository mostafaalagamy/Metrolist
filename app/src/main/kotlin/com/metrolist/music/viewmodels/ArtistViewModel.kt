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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
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
                
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                
                YouTube.artist(artistId)
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
                        hasError = true
                        artistPage = null
                        reportException(throwable)
                    }
            } catch (e: Exception) {
                hasError = true
                artistPage = null
                reportException(e)
            } finally {
                isLoading = false
            }
        }
    }
    
    fun retry() {
        fetchArtistsFromYTM()
    }
}
