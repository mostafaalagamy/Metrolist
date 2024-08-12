package com.metrolist.music.ui.utils

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Trick: generate two [AppBarScrollBehavior] with synchronized states.
 * Each state can have different heightOffsetLimit.
 *
 * Reason: [SearchBar] and [TopAppBar] have different heightOffsetLimit.
 * Using same heightOffsetLimit will cause "Size(720 x -68) is out of range".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarScrollBehavior(
    state1: TopAppBarState = rememberTopAppBarState(),
    state2: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): Pair<AppBarScrollBehavior, AppBarScrollBehavior> =
    Pair(
        AppBarScrollBehavior(
            state = state1,
            states = listOf(state1, state2),
            snapAnimationSpec = snapAnimationSpec,
            flingAnimationSpec = flingAnimationSpec,
            canScroll = canScroll,
        ),
        AppBarScrollBehavior(
            state = state2,
            states = listOf(state1, state2),
            snapAnimationSpec = snapAnimationSpec,
            flingAnimationSpec = flingAnimationSpec,
            canScroll = canScroll,
        ),
    )

@ExperimentalMaterial3Api
class AppBarScrollBehavior constructor(
    override val state: TopAppBarState,
    val states: List<TopAppBarState>,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = true
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) {
                    if (consumed.y == 0f && available.y > 0f) {
                        // Reset the total content offset to zero when scrolling all the way down.
                        // This will eliminate some float precision inaccuracies.
                        state.contentOffset = 0f
                    }
                }
                state.heightOffset += consumed.y
                return Offset.Zero
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun AppBarScrollBehavior.resetHeightOffset() {
    if (states.any { it.heightOffset != 0f }) {
        animate(
            initialValue = state.heightOffset,
            targetValue = 0f,
        ) { value, _ ->
            states.forEach {
                it.heightOffset = value
            }
        }
    }
}
