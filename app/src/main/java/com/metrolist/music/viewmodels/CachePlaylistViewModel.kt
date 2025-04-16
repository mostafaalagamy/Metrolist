package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.di.PlayerCache
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.utils.YTPlayerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject
import androidx.media3.datasource.cache.SimpleCache

@HiltViewModel
class CachePlaylistViewModel @Inject constructor(
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache
) : ViewModel() {

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

                database.query {
                    songs.forEach { song ->
                        val songId = song.song.id

                        if (song.format == null) {
                            runCatching {
                                val result = YTPlayerUtils.playerResponseForPlayback(songId)
                                val format = result.getOrThrow().format

                                upsert(
                                    FormatEntity(
                                        id = songId,
                                        itag = format.itag,
                                        mimeType = format.mimeType.split(";")[0],
                                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                                        bitrate = format.bitrate,
                                        sampleRate = format.audioSampleRate,
                                        contentLength = format.contentLength!!,
                                        loudnessDb = result.getOrThrow().audioConfig?.loudnessDb,
                                        playbackUrl = result.getOrThrow().playbackTracking?.videostatsPlaybackUrl?.baseUrl
                                    )
                                )
                            }
                        }

                        val updated = database.getSongById(songId)
                        val contentLength = updated?.format?.contentLength

                        if (contentLength != null && playerCache.isCached(songId, 0, contentLength)) {
                            if (updated.song.dateDownload == null) {
                                update(updated.song.copy(dateDownload = LocalDateTime.now()))
                            }

                            completeSongs.add(updated)
                        }
                    }
                }

                _cachedSongs.value = completeSongs
                    .filter { it.song.dateDownload != null }
                    .sortedByDescending { it.song.dateDownload }

                delay(1000)
            }
        }
    }

    fun removeSongFromCache(songId: String) {
        playerCache.removeResource(songId)
    }
}
