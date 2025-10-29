package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.filterWhitelisted
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
 
@HiltViewModel
class BrowseViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val browseId: String? = savedStateHandle.get<String>("browseId")

    val items = MutableStateFlow<List<YTItem>?>(emptyList())
    val title = MutableStateFlow<String?>("")

    init {
        viewModelScope.launch {
            browseId?.let {
                YouTube.browse(browseId, null).onSuccess { result ->
                    // Store the title
                    title.value = result.title

                    // Flatten the nested structure to get all YTItems
                    val allItems = result.items.flatMap { it.items }
                    items.value = allItems
                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                        .filterWhitelisted(database)
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }
}
