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
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.filterWhitelisted
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val trendingSongs = MutableStateFlow<List<YTItem>>(emptyList())
    val featuredAlbums = MutableStateFlow<List<com.metrolist.innertube.models.AlbumItem>>(emptyList())
    val featuredArtists = MutableStateFlow<List<com.metrolist.innertube.models.ArtistItem>>(emptyList())
    val isNewUser = MutableStateFlow(true)

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
                accountPlaylists.value = it.items
                    .filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
                    .filterWhitelisted(database)
                    .filterIsInstance<PlaylistItem>()
            }.onFailure {
                reportException(it)
            }
        }

        YouTube.home().onSuccess { page ->
            homePage.value = page.copy(
                sections = page.sections.mapNotNull { section ->
                    val filteredItems = section.items
                        .filterExplicit(hideExplicit)
                        .filterWhitelisted(database)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
        }.onFailure {
            reportException(it)
        }

        YouTube.explore().onSuccess { page ->
            val rawAlbums = page.newReleaseAlbums
            val afterExplicit = rawAlbums.filterExplicit(hideExplicit)
            val afterWhitelist = afterExplicit.filterWhitelisted(database)
            val finalAlbums = afterWhitelist.filterIsInstance<com.metrolist.innertube.models.AlbumItem>()

            Timber.d("HomeViewModel: New Release Albums - raw=${rawAlbums.size}, afterExplicit=${afterExplicit.size}, afterWhitelist=${afterWhitelist.size}, final=${finalAlbums.size}")

            explorePage.value = page.copy(
                newReleaseAlbums = finalAlbums
            )
        }.onFailure {
            Timber.e(it, "HomeViewModel: Failed to load explore page")
            reportException(it)
        }

        // Detect if user is new (no history-based content)
        val hasQuickPicks = quickPicks.value?.isNotEmpty() == true
        val hasKeepListening = keepListening.value?.isNotEmpty() == true
        isNewUser.value = !hasQuickPicks && !hasKeepListening

        Timber.d("HomeViewModel: New user detection - hasQuickPicks=$hasQuickPicks, hasKeepListening=$hasKeepListening, isNewUser=${isNewUser.value}")

        // Load featured content from whitelisted artists (always, for all users)
        Timber.d("HomeViewModel: Loading featured content from whitelisted artists")

        // Get 15 random whitelisted artist IDs
        val randomArtistIds = database.getRandomWhitelistedArtistIds(15)
        Timber.d("HomeViewModel: Fetched ${randomArtistIds.size} random whitelisted artist IDs")

        if (randomArtistIds.isNotEmpty()) {
            val albums = mutableListOf<com.metrolist.innertube.models.AlbumItem>()
            val artists = mutableListOf<com.metrolist.innertube.models.ArtistItem>()

            // Fetch artist pages and extract content
            randomArtistIds.take(15).forEach { artistId ->
                YouTube.artist(artistId).onSuccess { artistPage ->
                    // Add the artist themselves
                    artists.add(artistPage.artist)

                    // Extract albums from the artist page
                    val artistAlbums = artistPage.sections
                        .flatMap { it.items }
                        .filterIsInstance<com.metrolist.innertube.models.AlbumItem>()
                        .take(2) // Take top 2 albums per artist

                    albums.addAll(artistAlbums)
                    Timber.d("HomeViewModel: Artist ${artistPage.artist.title} - added ${artistAlbums.size} albums")
                }.onFailure {
                    Timber.w(it, "HomeViewModel: Failed to fetch artist $artistId")
                }
            }

            featuredAlbums.value = albums.shuffled().take(20)
            featuredArtists.value = artists.shuffled().take(20)

            Timber.d("HomeViewModel: Featured content - ${featuredAlbums.value.size} albums, ${featuredArtists.value.size} artists")
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        allYtItems.value = homePage.value?.sections?.flatMap { it.items }.orEmpty()

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
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).mapNotNull { section ->
                    val filteredItems = section.items
                        .filterExplicit(hideExplicit)
                        .filterWhitelisted(database)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
            _isLoadingMore.value = false
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

            val isSyncEnabled = context.dataStore.get(YtmSyncKey, true)
            if (isSyncEnabled) {
                // Sync artist whitelist FIRST before loading home content
                // This ensures whitelist filtering works correctly for new users
                syncUtils.syncArtistWhitelist()
            }

            load()

            // Run other syncs in background after load completes
            if (isSyncEnabled) {
                viewModelScope.launch(Dispatchers.IO) {
                    syncUtils.syncLikedSongs()
                    syncUtils.syncLibrarySongs()
                    syncUtils.syncUploadedSongs()
                    syncUtils.syncLikedAlbums()
                    syncUtils.syncUploadedAlbums()
                    syncUtils.syncArtistsSubscriptions()
                    syncUtils.syncSavedPlaylists()
                }
            }
        }

        // Observe Quick Picks changes continuously
        viewModelScope.launch(Dispatchers.IO) {
            quickPicksEnum.flatMapLatest { mode ->
                when (mode) {
                    QuickPicks.QUICK_PICKS -> database.quickPicks().map { it.shuffled().take(20) }
                    QuickPicks.LAST_LISTEN -> database.events().flatMapLatest { events ->
                        val song = events.firstOrNull()?.song
                        if (song != null && database.hasRelatedSongs(song.id)) {
                            database.getRelatedSongs(song.id).map { it.shuffled().take(20) }
                        } else {
                            flowOf(emptyList())
                        }
                    }
                }
            }.collect { songs ->
                quickPicks.value = songs
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
