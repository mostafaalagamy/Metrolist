package com.metrolist.music.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.metrolist.music.R



@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    val iconInactive: ImageVector,
    val iconActive: ImageVector,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconInactive = Icons.Outlined.Home,
        iconActive = Icons.Filled.Home,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconInactive = Icons.Filled.Search,
        iconActive = Icons.Filled.Search,
        route = "search"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconInactive = Icons.Outlined.LibraryMusic,
        iconActive = Icons.Filled.LibraryBooks,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library)
    }
}
