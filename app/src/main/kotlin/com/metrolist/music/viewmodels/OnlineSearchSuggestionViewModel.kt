package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.filterWhitelisted
import com.metrolist.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history,
                            )
                        }
                    } else {
                        val result = YouTube.searchSuggestions(query).getOrNull()
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

                        // Filter items with whitelist (suspend function)
                        val filteredItems = result
                            ?.recommendedItems
                            ?.distinctBy { it.id }
                            ?.filterExplicit(hideExplicit)
                            ?.filterWhitelisted(database)
                            .orEmpty()

                        // Get whitelisted artist names for filtering query suggestions
                        val whitelistedArtists = database.getAllWhitelistedArtists().map { entries ->
                            entries.map { it.artistName.lowercase() }
                        }

                        database
                            .searchHistory(query)
                            .map { it.take(3) }
                            .flatMapLatest { history ->
                                whitelistedArtists.map { artistNames ->
                                    SearchSuggestionViewState(
                                        history = history,
                                        suggestions =
                                        result
                                            ?.queries
                                            ?.filter { suggestionQuery ->
                                                // Only show suggestions that contain a whitelisted artist name
                                                val lowerQuery = suggestionQuery.lowercase()
                                                artistNames.any { artistName ->
                                                    lowerQuery.contains(artistName)
                                                }
                                            }
                                            ?.filter { suggestionQuery ->
                                                history.none { it.query == suggestionQuery }
                                            }.orEmpty(),
                                        items = filteredItems,
                                    )
                                }
                            }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)
