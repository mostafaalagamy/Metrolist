package com.malopieds.innertune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.pages.MoodAndGenres
import com.malopieds.innertune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
    @Inject
    constructor() : ViewModel() {
        val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

        init {
            viewModelScope.launch {
                YouTube
                    .moodAndGenres()
                    .onSuccess {
                        moodAndGenres.value = it
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
