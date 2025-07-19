package com.metrolist.music.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.metrolist.music.R
import com.metrolist.music.ui.theme.AppIcons

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    val iconIdInactive: ImageVector,
    val iconIdActive: ImageVector,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = AppIcons.HomeOutlined,
        iconIdActive = AppIcons.HomeFilled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = AppIcons.Search,
        iconIdActive = AppIcons.Search,
        route = "search"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = AppIcons.LibraryMusicOutlined,
        iconIdActive = AppIcons.LibraryMusicFilled,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library)
    }
}
