package com.zionhuang.music.ui.screens.playlist

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.music.constants.MyTopFilter
import com.zionhuang.music.constants.MyTopTypeKey
import com.zionhuang.music.constants.PlaylistSongSortType
import com.zionhuang.music.constants.ShowLyricsKey
import com.zionhuang.music.constants.SongSortType
import com.zionhuang.music.constants.StatPeriod
import com.zionhuang.music.constants.TopSize
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.extensions.reversed
import com.zionhuang.music.extensions.toEnum
import com.zionhuang.music.playback.DownloadUtil
import com.zionhuang.music.utils.dataStore
import com.zionhuang.music.utils.rememberPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopPlaylistViewModel  @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val top = savedStateHandle.get<String>("top")!!

    val topPeriod = MutableStateFlow(MyTopFilter.ALL_TIME)

    @OptIn(ExperimentalCoroutinesApi::class)
    val topSongs = topPeriod.flatMapLatest { period ->
        database.mostPlayedSongs(period.toTimeMillis(), top.toInt())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

}

