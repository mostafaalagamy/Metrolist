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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val allProviders =
        listOf(
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            BetterLyricsProvider,
            AppleMusicLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return cached.lyrics
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return LYRICS_NOT_FOUND
        }

        val preferredProviderEnum = context.dataStore.data.map {
            it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
        }.first()

        val preferredProviderFromSettings = allProviders.find { it.name.equals(preferredProviderEnum.name, true) }
        val enabledProviders = allProviders.filter { it.isEnabled(context) }

        val sortedProviders = enabledProviders.sortedBy { it != preferredProviderFromSettings }

        val preferredProvider = sortedProviders.firstOrNull()

        if (preferredProvider != null) {
            try {
                val result = withTimeoutOrNull(PREFERRED_PROVIDER_TIMEOUT_MS) {
                    preferredProvider.getLyrics(
                        mediaMetadata.id,
                        mediaMetadata.title,
                        mediaMetadata.artists.joinToString { it.name },
                        mediaMetadata.duration
                    )
                }

                if (result?.isSuccess == true) {
                    val lyrics = result.getOrThrow()
                    if (lyrics.isNotEmpty()) {
                        return lyrics
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            }
        }

        val otherProviders = sortedProviders.drop(1)

        if (otherProviders.isNotEmpty()) {
            val results = coroutineScope {
                otherProviders.map { provider ->
                    async {
                        try {
                            provider.getLyrics(
                                mediaMetadata.id,
                                mediaMetadata.title,
                                mediaMetadata.artists.joinToString { it.name },
                                mediaMetadata.duration
                            )
                        } catch (e: Exception) {
                            reportException(e)
                            Result.failure(e)
                        }
                    }
                }.awaitAll()
            }

            for (result in results) {
                if (result.isSuccess) {
                    val lyrics = result.getOrThrow()
                    if (lyrics.isNotEmpty()) {
                        return lyrics
                    }
                }
            }
        }

        return LYRICS_NOT_FOUND
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
    ): List<LyricsResult> {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        val cachedResults = cache.get(cacheKey)
        if (cachedResults != null) {
            return cachedResults
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return emptyList()
        }

        val allResults = mutableListOf<LyricsResult>()
        val job = CoroutineScope(SupervisorJob()).async {
            allProviders.filter { it.isEnabled(context) }.map { provider ->
                async {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                            val result = LyricsResult(provider.name, lyrics)
                            synchronized(allResults) {
                                allResults.add(result)
                            }
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }.awaitAll()
        }
        job.await()
        cache.put(cacheKey, allResults)
        return allResults
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private const val PREFERRED_PROVIDER_TIMEOUT_MS = 15000L
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
