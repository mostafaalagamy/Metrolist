/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.DefaultThemeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel : ViewModel() {
    // Theme state flows
    private val _darkMode = MutableStateFlow(DarkMode.AUTO)
    val darkMode: StateFlow<DarkMode> = _darkMode.asStateFlow()

    private val _pureBlack = MutableStateFlow(false)
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _selectedThemeColorInt = MutableStateFlow(DefaultThemeColor.hashCode())
    val selectedThemeColorInt: StateFlow<Int> = _selectedThemeColorInt.asStateFlow()

    fun updateDarkMode(mode: DarkMode) {
        _darkMode.value = mode
    }

    fun updatePureBlack(enabled: Boolean) {
        _pureBlack.value = enabled
    }

    fun updateThemeColor(colorInt: Int) {
        _selectedThemeColorInt.value = colorInt
    }
}
