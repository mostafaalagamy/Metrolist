package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.SimilarRecommendation
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    // Track if we're currently processing account data
    private var isProcessingAccountData = false

    private suspend fun getQuickPicks() {
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().shuffled().take(20)
                }
            }
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        getQuickPicks()
        forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        if (YouTube.cookie != null) {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
        }

        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).onSuccess { page ->
                    items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                    items += page.sections.lastOrNull()?.items.orEmpty()
                }
                SimilarRecommendation(
                    title = it,
                    items = items.filterExplicit(hideExplicit).shuffled().ifEmpty { return@mapNotNull null }
                )
            }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
            .filter { it.album != null }
            .shuffled().take(2)
            .mapNotNull { song ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (page.songs.shuffled().take(8) +
                            page.albums.shuffled().take(4) +
                            page.artists.shuffled().take(4) +
                            page.playlists.shuffled().take(4))
                        .filterExplicit(hideExplicit)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }
        similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        YouTube.home().onSuccess { page ->
            homePage.value = page.copy(
                sections = page.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit))
                }
            )
        }.onFailure {
            reportException(it)
        }

        YouTube.explore().onSuccess { page ->
            explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()

        isLoading.value = false
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit))
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val nextSections = YouTube.home(params = chip?.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit))
                }
            )
            selectedChip.value = chip
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()
            
            load()

            val isSyncEnabled = context.dataStore.get(YtmSyncKey, true)
            if (isSyncEnabled) {
                syncUtils.runAllSyncs()
            }
        }

        // Listen for cookie changes and reload account data
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    // Avoid processing if already processing
                    if (isProcessingAccountData) return@collect
                    
                    // Always process cookie changes, even if same value (for logout/login scenarios)
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    
                    try {
                        if (cookie != null && cookie.isNotEmpty()) {
                            
                            // Update YouTube.cookie manually to ensure it's set
                            YouTube.cookie = cookie
                            
                            // Fetch new account data
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                            }.onFailure {
                                reportException(it)
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }
    }
}
