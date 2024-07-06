package com.malopieds.innertune.lyrics

import android.content.Context
import android.util.LruCache
import com.malopieds.innertune.constants.PreferredLyricsProvider
import com.malopieds.innertune.constants.PreferredLyricsProviderKey
import com.malopieds.innertune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.malopieds.innertune.extensions.toEnum
import com.malopieds.innertune.models.MediaMetadata
import com.malopieds.innertune.utils.dataStore
import com.malopieds.innertune.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LyricsHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var lyricsProviders =
            listOf(LrcLibLyricsProvider, KuGouLyricsProvider, YouTubeSubtitleLyricsProvider, YouTubeLyricsProvider)
        val preferred =
            context.dataStore.data
                .map {
                    it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
                }.distinctUntilChanged()
                .map {
                    lyricsProviders =
                        if (it == PreferredLyricsProvider.LRCLIB) {
                            listOf(LrcLibLyricsProvider, KuGouLyricsProvider, YouTubeSubtitleLyricsProvider, YouTubeLyricsProvider)
                        } else {
                            listOf(KuGouLyricsProvider, LrcLibLyricsProvider, YouTubeSubtitleLyricsProvider, YouTubeLyricsProvider)
                        }
                }
        private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)

        suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
            val cached = cache.get(mediaMetadata.id)?.firstOrNull()
            if (cached != null) {
                return cached.lyrics
            }
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    provider
                        .getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                        ).onSuccess { lyrics ->
                            return lyrics
                        }.onFailure {
                            reportException(it)
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
            callback: (LyricsResult) -> Unit,
        ) {
            val cacheKey = "$songArtists-$songTitle".replace(" ", "")
            cache.get(cacheKey)?.let { results ->
                results.forEach {
                    callback(it)
                }
                return
            }
            val allResult = mutableListOf<LyricsResult>()
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                        val result = LyricsResult(provider.name, lyrics)
                        allResult += result
                        callback(result)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        companion object {
            private const val MAX_CACHE_SIZE = 3
        }
    }

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
