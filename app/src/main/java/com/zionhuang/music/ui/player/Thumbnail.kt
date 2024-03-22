package com.zionhuang.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.constants.PlayerHorizontalPadding
import com.zionhuang.music.constants.ShowLyricsKey
import com.zionhuang.music.constants.ThumbnailCornerRadius
import com.zionhuang.music.extensions.metadata
import com.zionhuang.music.ui.component.Lyrics
import com.zionhuang.music.ui.utils.SnapLayoutInfoProvider
import com.zionhuang.music.utils.rememberPreference
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val windows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val error by playerConnection.error.collectAsState()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, false)

    val pagerState = rememberPagerState(
        initialPage = currentWindowIndex.takeIf { it != -1 } ?: 0
    )

    val snapLayoutInfoProvider = remember(pagerState) {
        SnapLayoutInfoProvider(
            pagerState = pagerState,
            positionInLayout = { _, _ -> 0f }
        )
    }

    LaunchedEffect(pagerState, currentWindowIndex) {
        try {
            pagerState.scrollToPage(currentWindowIndex)
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect {
            if (!pagerState.isScrollInProgress) {
                playerConnection.player.seekToDefaultPosition(it)
            }
        }
    }

    DisposableEffect(showLyrics) {
        println(currentWindowIndex)
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            windows.takeIf { it.isNotEmpty() }?.let { windows ->
                HorizontalPager(
                    state = pagerState,
                    flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                    pageCount = windows.size,
                    key = { windows[it].uid.hashCode() },
                    beyondBoundsPageCount = 2,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) { index ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = PlayerHorizontalPadding)
                    ) {
                        val model = if (currentWindowIndex == index){
                            mediaMetadata?.thumbnailUrl
                        } else {
                            windows[currentWindowIndex].mediaItem.metadata?.thumbnailUrl
                        }
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { offset ->
                                            if (offset.x < size.width / 2) {
                                                playerConnection.player.seekBack()
                                            } else {
                                                playerConnection.player.seekForward()
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Lyrics(
                sliderPositionProvider = sliderPositionProvider
            )
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center)
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare
                )
            }
        }
    }
}