package com.moxxaxx.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.BrowseEndpoint
import com.moxxaxx.music.models.ItemsPage
import com.moxxaxx.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistItemsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val browseId = savedStateHandle.get<String>("browseId")!!
        private val params = savedStateHandle.get<String>("params")

        val title = MutableStateFlow("")
        val itemsPage = MutableStateFlow<ItemsPage?>(null)

        init {
            viewModelScope.launch {
                YouTube
                    .artistItems(
                        BrowseEndpoint(
                            browseId = browseId,
                            params = params,
                        ),
                    ).onSuccess { artistItemsPage ->
                        title.value = artistItemsPage.title
                        itemsPage.value =
                            ItemsPage(
                                items = artistItemsPage.items,
                                continuation = artistItemsPage.continuation,
                            )
                    }.onFailure {
                        reportException(it)
                    }
            }
        }

        fun loadMore() {
            viewModelScope.launch {
                val oldItemsPage = itemsPage.value ?: return@launch
                val continuation = oldItemsPage.continuation ?: return@launch
                YouTube
                    .artistItemsContinuation(continuation)
                    .onSuccess { artistItemsContinuationPage ->
                        itemsPage.update {
                            ItemsPage(
                                items = (oldItemsPage.items + artistItemsContinuationPage.items).distinctBy { it.id },
                                continuation = artistItemsContinuationPage.continuation,
                            )
                        }
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
