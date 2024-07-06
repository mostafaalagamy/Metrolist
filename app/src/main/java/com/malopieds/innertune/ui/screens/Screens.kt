package com.malopieds.innertune.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.malopieds.innertune.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconId: Int,
    val route: String,
) {
    object Home : Screens(R.string.home, R.drawable.home, "home")

    object Explore : Screens(R.string.explore, R.drawable.explore, "explore")

    object Library : Screens(R.string.filter_library, R.drawable.library_music, "library")

    companion object {
        val MainScreens = listOf(Home, Explore, Library)
    }
}
