package com.malopieds.innertune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertune.db.MusicDatabase
import com.malopieds.innertune.db.entities.LyricsEntity
import com.malopieds.innertune.lyrics.LyricsHelper
import com.malopieds.innertune.lyrics.LyricsResult
import com.malopieds.innertune.models.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
    @Inject
    constructor(
        private val lyricsHelper: LyricsHelper,
        val database: MusicDatabase,
    ) : ViewModel() {
        private var job: Job? = null
        val results = MutableStateFlow(emptyList<LyricsResult>())
        val isLoading = MutableStateFlow(false)

        fun search(
            mediaId: String,
            title: String,
            artist: String,
            duration: Int,
        ) {
            isLoading.value = true
            results.value = emptyList()
            job?.cancel()
            job =
                viewModelScope.launch(Dispatchers.IO) {
                    lyricsHelper.getAllLyrics(mediaId, title, artist, duration) { result ->
                        results.update {
                            it + result
                        }
                    }
                    isLoading.value = false
                }
        }

        fun cancelSearch() {
            job?.cancel()
            job = null
        }

        fun refetchLyrics(
            mediaMetadata: MediaMetadata,
            lyricsEntity: LyricsEntity?,
        ) {
            database.query {
                lyricsEntity?.let(::delete)
                val lyrics =
                    runBlocking {
                        lyricsHelper.getLyrics(mediaMetadata)
                    }
                upsert(LyricsEntity(mediaMetadata.id, lyrics))
            }
        }
    }
