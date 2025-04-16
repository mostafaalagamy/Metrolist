package com.metrolist.music.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.di.PlayerCache
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.enumPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import androidx.media3.datasource.cache.SimpleCache

@HiltViewModel
class CachePlaylistViewModel @Inject constructor(
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache,
    @ApplicationContext context: Context
) : ViewModel() {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)

    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val cachedIds = playerCache.keys.mapNotNull { it?.toString() }.toSet()
                val downloadedIds = downloadCache.keys.mapNotNull { it?.toString() }.toSet()
                val pureCacheIds = cachedIds.subtract(downloadedIds)

                val songs = if (pureCacheIds.isNotEmpty()) {
                    database.getSongsByIds(pureCacheIds.toList())
                } else {
                    emptyList()
                }

                val completeSongs = mutableListOf<Song>()

                for (song in songs) {
                    val songId = song.song.id
                    var updatedFormat: FormatEntity? = null

                    if (song.format == null) {
                        try {
                            val result = YTPlayerUtils.playerResponseForPlayback(
                                mediaId = songId,
                                videoId = songId,
                                playedFormat = null,
                                audioQuality = audioQuality,
                                connectivityManager = connectivityManager
                            ).getOrThrow()

                            val format = result.format
                            updatedFormat = FormatEntity(
                                id = songId,
                                itag = format.itag,
                                mimeType = format.mimeType.split(";")[0],
                                codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                                bitrate = format.bitrate,
                                sampleRate = format.audioSampleRate,
                                contentLength = format.contentLength!!,
                                loudnessDb = result.audioConfig?.loudnessDb,
                                playbackUrl = result.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                            )
                        } catch (_: Exception) {
                        }
                    }

                    database.query {
                        if (updatedFormat != null) {
                            upsert(updatedFormat)
                        }

                        val updated = getSongById(songId)
                        val contentLength = updated?.format?.contentLength

                        if (contentLength != null && playerCache.isCached(songId, 0, contentLength)) {
                            if (updated.song.dateDownload == null) {
                                update(updated.song.copy(dateDownload = LocalDateTime.now()))
                            }
                            completeSongs.add(updated)
                        }
                    }
                }

                _cachedSongs.update {
                    completeSongs
                        .filter { it.song.dateDownload != null }
                        .sortedByDescending { it.song.dateDownload }
                }

                delay(1000)
            }
        }
    }

    fun removeSongFromCache(songId: String) {
        playerCache.removeResource(songId)
    }
}
