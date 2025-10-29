package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.filterWhitelisted
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess { page ->
                                val filteredPage = page.filterExplicit(
                                    context.dataStore.get(
                                        HideExplicitKey,
                                        false,
                                    ),
                                )
                                summaryPage = SearchSummaryPage(
                                    summaries = filteredPage.summaries.mapNotNull { summary ->
                                        val filteredItems = summary.items.filterWhitelisted(database)
                                        if (filteredItems.isEmpty()) null else summary.copy(items = filteredItems)
                                    }
                                )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            )
                                            .filterWhitelisted(database),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items)
                        .distinctBy { it.id }
                        .filterExplicit(hideExplicit)
                        .filterWhitelisted(database),
                    searchResult.continuation
                )
            }
        }
    }
}
