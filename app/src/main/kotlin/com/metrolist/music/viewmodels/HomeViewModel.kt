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
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.*
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.SimilarRecommendation
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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

    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    // Account display info
    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    private suspend fun getQuickPicks(){
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> songLoad()
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        getQuickPicks()

        forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2

        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
            .first().shuffled().take(10)

        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
            .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)

        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
            .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
            .shuffled().take(5)

        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }

        if (YouTube.cookie != null) {
            YouTube.accountInfo().onSuccess { info ->
                accountName.value = info.name
                accountImageUrl.value = info.thumbnailUrl
            }.onFailure {
                reportException(it)
            }

            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                val lists = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
                accountPlaylists.value = lists
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
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                    ?: return@mapNotNull null
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

        // Explore section with sorting by favorite artist
        YouTube.explore().onSuccess { page ->
            val artists: MutableMap<Int, String> = mutableMapOf()
            val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
            database.allArtistsByPlayTime().first().let { list ->
                var favIndex = 0
                for ((artistsIndex, artist) in list.withIndex()) {
                    artists[artistsIndex] = artist.id
                    if (artist.artist.bookmarkedAt != null) {
                        favouriteArtists[favIndex] = artist.id
                        favIndex++
                    }
                }
            }
            explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums
                    .sortedBy { album ->
                        val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                        val firstArtistKey = artistIds.firstNotNullOfOrNull { artistId ->
                            if (artistId in favouriteArtists.values) {
                                favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                            } else {
                                artists.entries.firstOrNull { it.value == artistId }?.key
                            }
                        } ?: Int.MAX_VALUE
                        firstArtistKey
                    }.filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()

        isLoading.value = false
    }

    private suspend fun songLoad() {
        val song = database.events().first().firstOrNull()?.song
        if (song != null) {
            if (database.hasRelatedSongs(song.id)) {
                val relatedSongs = database.getRelatedSongs(song.id).first().shuffled().take(20)
                quickPicks.value = relatedSongs
            }
        }
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
            load()

            val isSyncEnabled = context.dataStore.data
                .map { it[YtmSyncKey] ?: true }
                .distinctUntilChanged()
                .first()

            if (isSyncEnabled) {
                supervisorScope {
                    launch { syncUtils.syncLikedSongs() }
                    launch { syncUtils.syncLibrarySongs() }
                    launch { syncUtils.syncSavedPlaylists() }
                    launch { syncUtils.syncLikedAlbums() }
                    launch { syncUtils.syncArtistsSubscriptions() }
                }
            }
        }
    }
}
