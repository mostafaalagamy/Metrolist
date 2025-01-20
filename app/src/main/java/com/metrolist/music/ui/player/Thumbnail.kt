package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.ShowLyricsKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current
    val context = LocalContext.current

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
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    val layoutDirection = LocalLayoutDirection.current

    fun updateImagePreview(offsetX: Float) {
        val threshold = 100f
        when {
            offsetX > threshold -> {
                isPreviewingNextSong = true
                previewImage =
                    playerConnection.player.previousMediaItemIndex.takeIf { it != -1 }?.let {
                        playerConnection.player.getMediaItemAt(it).mediaMetadata.artworkUri?.toString()
                    }
            }

            offsetX < -threshold -> {
                isPreviewingNextSong = true
                previewImage = playerConnection.player.nextMediaItemIndex.takeIf { it != -1 }?.let {
                    playerConnection.player.getMediaItemAt(it).mediaMetadata.artworkUri?.toString()
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
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    offsetX += adjustedDragAmount
                                    updateImagePreview(offsetX)
                                }
                            },
                            onDragEnd = {
                                val threshold = 0.10f * currentView.width

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
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                ) {
                    // Blurred background layer
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                renderEffect = BlurEffect(
                                    radiusX = 75f,
                                    radiusY = 75f
                                ),
                                alpha = 0.5f
                            )
                    )

                    // Main thumbnail image
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        val currentPosition =
                                            playerConnection.player.currentPosition
                                        if ((layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                            (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)
                                        ) {
                                            playerConnection.player.seekTo(
                                                (currentPosition - 5000).coerceAtLeast(
                                                    0
                                                )
                                            )
                                            seekDirection =
                                                context.getString(R.string.seek_backward)
                                        } else {
                                            playerConnection.player.seekTo(
                                                (currentPosition + 5000).coerceAtMost(
                                                    playerConnection.player.duration
                                                )
                                            )
                                            seekDirection = context.getString(R.string.seek_forward)
                                        }
                                        showSeekEffect = true
                                    }
                                )
                            }
                    )
                }

                LaunchedEffect(showSeekEffect) {
                    if (showSeekEffect) {
                        delay(1000)
                        showSeekEffect = false
                    }
                }

                AnimatedVisibility(
                    visible = showSeekEffect,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = seekDirection,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                previewImage?.let {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    if (offsetX > 0) (offsetX - currentView.width).roundToInt()
                                    else (offsetX + currentView.width).roundToInt(), 0
                                )
                            }
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                    ) {
                        // Blurred background for preview image
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    renderEffect = BlurEffect(
                                        radiusX = 75f,
                                        radiusY = 75f
                                    ),
                                    alpha = 0.5f
                                )
                        )

                        // Preview image
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
