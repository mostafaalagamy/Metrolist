package com.metrolist.music.lyrics

import android.content.Context
import android.util.LruCache
import com.metrolist.music.constants.PreferredLyricsProvider
import com.metrolist.music.constants.PreferredLyricsProviderKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private var lyricsProviders =
        listOf(
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )

    val preferred =
        context.dataStore.data
            .map {
                it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
            }.distinctUntilChanged()
            .map {
                lyricsProviders =
                    if (it == PreferredLyricsProvider.LRCLIB) {
                        listOf(
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                    } else {
                        listOf(
                            KuGouLyricsProvider,
                            LrcLibLyricsProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                    }
            }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return cached.lyrics
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            // Still proceed but return not found to avoid hanging
            return LYRICS_NOT_FOUND
        }

        val scope = CoroutineScope(SupervisorJob())
        val deferred = scope.async {
            for (provider in lyricsProviders) {
                if (provider.isEnabled(context)) {
                    try {
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                        )
                        result.onSuccess { lyrics ->
                            return@async lyrics
                        }.onFailure {
                            reportException(it)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        reportException(e)
                    }
                }
            }
            return@async LYRICS_NOT_FOUND
        }

        val lyrics = deferred.await()
        scope.cancel()
        return lyrics
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            // Still try to proceed in case of false negative
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
