package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.ArtistViewTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.LibraryArtistGridItem
import com.metrolist.music.ui.component.LibraryArtistListItem
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.WhitelistedArtistGridItem
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.WhitelistedArtistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhitelistedArtistsScreen(
    navController: NavController,
    viewModel: WhitelistedArtistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    val artists by viewModel.allArtists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val searchContent = @Composable {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_artists)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.artists),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(
                    R.plurals.n_artist,
                    artists.size,
                    artists.size
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp),
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        },
                    ),
                    contentDescription = null,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "search",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        searchContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (artists.isEmpty()) {
                        item(key = "empty_placeholder") {
                            EmptyPlaceholder(
                                icon = R.drawable.artist,
                                text = if (searchQuery.isEmpty()) {
                                    stringResource(R.string.library_artist_empty)
                                } else {
                                    stringResource(R.string.no_results_found)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    items(
                        items = artists.distinctBy { it.artist.name },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_ARTIST },
                    ) { artist ->
                        LibraryArtistListItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            modifier = Modifier.animateItem(),
                            artist = artist
                        )
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "search",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        searchContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (artists.isEmpty()) {
                        item(
                            key = "empty_placeholder",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            EmptyPlaceholder(
                                icon = R.drawable.artist,
                                text = if (searchQuery.isEmpty()) {
                                    stringResource(R.string.library_artist_empty)
                                } else {
                                    stringResource(R.string.no_results_found)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    items(
                        items = artists.distinctBy { it.artist.name },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_ARTIST },
                    ) { artist ->
                        WhitelistedArtistGridItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            modifier = Modifier.animateItem(),
                            artist = artist
                        )
                    }
                }
        }
    }
}
