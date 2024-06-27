@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.malopieds.innertune.ui.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
@ExperimentalFoundationApi
fun <T> HorizontalPager(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((item: T) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        Orientation.Horizontal
    ),
    pageContent: @Composable (item: T) -> Unit,
) {
    Pager(
        modifier = modifier,
        state = state,
        items = items,
        pageSpacing = pageSpacing,
        userScrollEnabled = userScrollEnabled,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSize = pageSize,
        flingBehavior = flingBehavior,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        pageContent = pageContent
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> Pager(
    modifier: Modifier,
    state: PagerState,
    items: List<T>,
    pageSize: PageSize,
    pageSpacing: Dp,
    orientation: Orientation,
    beyondBoundsPageCount: Int,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    contentPadding: PaddingValues,
    flingBehavior: SnapFlingBehavior,
    userScrollEnabled: Boolean,
    reverseLayout: Boolean,
    key: ((item: T) -> Any)?,
    pageNestedScrollConnection: NestedScrollConnection,
    pageContent: @Composable (item: T) -> Unit,
) {
    require(beyondBoundsPageCount >= 0) {
        "beyondBoundsPageCount should be greater than or equal to 0, " +
                "you selected $beyondBoundsPageCount"
    }

    val isVertical = orientation == Orientation.Vertical
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val calculatedContentPaddings = remember(contentPadding, orientation, layoutDirection) {
        calculateContentPaddings(
            contentPadding,
            orientation,
            layoutDirection
        )
    }

    val pagerFlingBehavior = remember(flingBehavior, state) {
        PagerWrapperFlingBehavior(flingBehavior, state)
    }

    LaunchedEffect(density, state, pageSpacing) {
        with(density) { state.pageSpacing = pageSpacing.roundToPx() }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .drop(1) // Initial scroll is false
            .collect { state.updateOnScrollStopped() }
    }

    val pagerSemantics = if (userScrollEnabled) {
        Modifier.pagerSemantics(state, isVertical)
    } else {
        Modifier
    }

    BoxWithConstraints(modifier = modifier.then(pagerSemantics)) {
        val mainAxisSize = if (isVertical) constraints.maxHeight else constraints.maxWidth
        // Calculates how pages are shown across the main axis
        val pageAvailableSize = remember(
            density,
            mainAxisSize,
            pageSpacing,
            calculatedContentPaddings
        ) {
            with(density) {
                val pageSpacingPx = pageSpacing.roundToPx()
                val contentPaddingPx = calculatedContentPaddings.roundToPx()
                with(pageSize) {
                    density.calculateMainAxisPageSize(
                        mainAxisSize - contentPaddingPx,
                        pageSpacingPx
                    )
                }.toDp()
            }
        }

        val horizontalAlignmentForSpacedArrangement =
            if (!reverseLayout) Alignment.Start else Alignment.End
        val verticalAlignmentForSpacedArrangement =
            if (!reverseLayout) Alignment.Top else Alignment.Bottom

        val lazyListState = remember(state) {
            val initialPageOffset =
                with(density) { pageAvailableSize.roundToPx() } * state.initialPageOffsetFraction
            LazyListState(state.initialPage, initialPageOffset.roundToInt()).also {
                state.loadNewState(it)
            }
        }

        LazyList(
            modifier = Modifier,
            state = lazyListState,
            contentPadding = contentPadding,
            flingBehavior = pagerFlingBehavior,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = Arrangement.spacedBy(
                pageSpacing,
                horizontalAlignmentForSpacedArrangement
            ),
            verticalArrangement = Arrangement.spacedBy(
                pageSpacing,
                verticalAlignmentForSpacedArrangement
            ),
            verticalAlignment = verticalAlignment,
            isVertical = isVertical,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            beyondBoundsItemCount = beyondBoundsPageCount
        ) {
            items(items = items, key = key) { item ->
                val pageMainAxisSizeModifier = if (isVertical) {
                    Modifier.height(pageAvailableSize)
                } else {
                    Modifier.width(pageAvailableSize)
                }
                Box(
                    modifier = Modifier
                        .then(pageMainAxisSizeModifier)
                        .nestedScroll(pageNestedScrollConnection),
                    contentAlignment = Alignment.Center
                ) {
                    pageContent(item)
                }
            }
        }
    }
}

private fun calculateContentPaddings(
    contentPadding: PaddingValues,
    orientation: Orientation,
    layoutDirection: LayoutDirection,
): Dp {

    val startPadding = if (orientation == Orientation.Vertical) {
        contentPadding.calculateTopPadding()
    } else {
        contentPadding.calculateLeftPadding(layoutDirection)
    }

    val endPadding = if (orientation == Orientation.Vertical) {
        contentPadding.calculateBottomPadding()
    } else {
        contentPadding.calculateRightPadding(layoutDirection)
    }

    return startPadding + endPadding
}

@OptIn(ExperimentalFoundationApi::class)
private class PagerWrapperFlingBehavior(
    val originalFlingBehavior: SnapFlingBehavior,
    val pagerState: PagerState,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return with(originalFlingBehavior) {
            performFling(initialVelocity) { remainingScrollOffset ->
                pagerState.snapRemainingScrollOffset = remainingScrollOffset
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("ComposableModifierFactory")
@Composable
private fun Modifier.pagerSemantics(state: PagerState, isVertical: Boolean): Modifier {
    val scope = rememberCoroutineScope()
    fun performForwardPaging(): Boolean {
        return if (state.canScrollForward) {
            scope.launch {
                state.animateToNextPage()
            }
            true
        } else {
            false
        }
    }

    fun performBackwardPaging(): Boolean {
        return if (state.canScrollBackward) {
            scope.launch {
                state.animateToPreviousPage()
            }
            true
        } else {
            false
        }
    }

    return this.then(Modifier.semantics {
        if (isVertical) {
            pageUp { performBackwardPaging() }
            pageDown { performForwardPaging() }
        } else {
            pageLeft { performBackwardPaging() }
            pageRight { performForwardPaging() }
        }
    })
}
