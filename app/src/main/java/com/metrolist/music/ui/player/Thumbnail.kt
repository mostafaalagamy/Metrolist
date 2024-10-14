package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.ShowLyricsKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt
import kotlin.math.absoluteValue

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    changeColor: Boolean = false,
    color: Color,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, false)
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)

    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isPreviewingNextSong by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<String?>(null) }

    val layoutDirection = LocalLayoutDirection.current

    fun updateImagePreview(offsetX: Float) {
        val threshold = 100f
        when {
            offsetX > threshold -> {
                isPreviewingNextSong = true
                previewImage = playerConnection.player.previousMediaItemIndex.takeIf { it != -1 }?.let {
                    playerConnection.player.getMediaItemAt(it)?.mediaMetadata?.artworkUri?.toString()
                }
            }
            offsetX < -threshold -> {
                isPreviewingNextSong = true
                previewImage = playerConnection.player.nextMediaItemIndex.takeIf { it != -1 }?.let {
                    playerConnection.player.getMediaItemAt(it)?.mediaMetadata?.artworkUri?.toString()
                }
            }
            else -> {
                isPreviewingNextSong = false
                previewImage = null
            }
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isPreviewingNextSong = true
                            },
                            onDragCancel = {
                                offsetX = 0f
                                isPreviewingNextSong = false
                                previewImage = null
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (swipeThumbnail) {
                                    val adjustedDragAmount = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    offsetX += adjustedDragAmount
                                    updateImagePreview(offsetX)
                                }
                            },
                            onDragEnd = {                    
                                val threshold = 0.25f * currentView.width

                                if (offsetX.absoluteValue > threshold) {
                                    if (offsetX > 0 && playerConnection.player.previousMediaItemIndex != -1) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (offsetX < 0 && playerConnection.player.nextMediaItemIndex != -1) {
                                        playerConnection.player.seekToNext()
                                    }
                                }
                                offsetX = 0f
                                isPreviewingNextSong = false
                                previewImage = null
                            },
                        )
                    },
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
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

                previewImage?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .offset {                              
                                IntOffset(
                                    if (offsetX > 0) (offsetX - currentView.width).roundToInt()
                                    else (offsetX + currentView.width).roundToInt(), 0
                                )
                            }
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Lyrics(
                sliderPositionProvider = sliderPositionProvider,
                changeColor = changeColor,
                color = color,
            )
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare,
                )
            }
        }
    }
} 
