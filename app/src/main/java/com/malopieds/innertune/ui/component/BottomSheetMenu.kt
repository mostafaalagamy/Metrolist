package com.malopieds.innertune.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.malopieds.innertune.ui.utils.top

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
) {
    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BackHandler {
            state.dismiss()
        }

        Spacer(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures {
                        state.dismiss()
                    }
                }
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .fillMaxSize()
        )
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(top = 48.dp)
                .clip(ShapeDefaults.Large.top())
                .background(background)
        ) {
            state.content(this)
        }
    }

    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            focusManager.clearFocus()
        }
    }
}