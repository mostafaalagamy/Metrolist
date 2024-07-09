package com.malopieds.innertune.ui.utils

import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import com.malopieds.innertune.ui.screens.Screens

val NavController.canNavigateUp: Boolean
    get() = currentDestination?.route != "home"

fun NavController.backToMain() {
    while (canNavigateUp && !Screens.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}
