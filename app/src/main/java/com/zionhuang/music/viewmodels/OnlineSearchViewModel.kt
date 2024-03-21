package com.zionhuang.music.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.pages.SearchSummaryPage
import com.zionhuang.music.models.ItemsPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
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
                        summaryPage = YouTube.searchSummary(query).getOrNull()
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        viewStateMap[filter.value] = YouTube.search(query, filter).getOrNull()?.let { result ->
                            ItemsPage(result.items.distinctBy { it.id }, result.continuation)
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
                val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
            }
        }
    }
}
