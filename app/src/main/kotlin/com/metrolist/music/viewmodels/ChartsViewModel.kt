package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.filterWhitelisted
import com.metrolist.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase
) : ViewModel() {
    private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
    val chartsPage = _chartsPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            YouTube.getChartsPage()
                .onSuccess { page ->
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    _chartsPage.value = page.copy(
                        sections = page.sections.map { section ->
                            section.copy(
                                items = section.items
                                    .filterExplicit(hideExplicit)
                                    .filterWhitelisted(database)
                            )
                        }
                    )
                }
                .onFailure { e ->
                    _error.value = "Failed to load charts: ${e.message}"
                }

            _isLoading.value = false
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            _chartsPage.value?.continuation?.let { continuation ->
                _isLoading.value = true
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                YouTube.getChartsPage(continuation)
                    .onSuccess { newPage ->
                        val filteredSections = newPage.sections.map { section ->
                            section.copy(
                                items = section.items
                                    .filterExplicit(hideExplicit)
                                    .filterWhitelisted(database)
                            )
                        }
                        _chartsPage.value = _chartsPage.value?.copy(
                            sections = _chartsPage.value?.sections.orEmpty() + filteredSections,
                            continuation = newPage.continuation
                        )
                    }
                    .onFailure { e ->
                        _error.value = "Failed to load more: ${e.message}"
                    }
                _isLoading.value = false
            }
        }
    }
}
