package com.metrolist.music.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.metrolist.music.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LibraryMusic

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    val iconIdInactive: ImageVector,
    val iconIdActive: ImageVector,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = Icons.Filled.Home,
        iconIdActive = Icons.Filled.Home,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = Icons.Filled.Search,
        iconIdActive = Icons.Filled.Search,
        route = "search"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = Icons.Filled.LibraryMusic,
        iconIdActive = Icons.Filled.LibraryMusic,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library)
    }
}
