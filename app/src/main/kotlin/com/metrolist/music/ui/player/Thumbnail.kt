package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)

    val coroutineScope = rememberCoroutineScope()
    val offsetXAnimatable = remember { Animatable(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

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
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    var displayedThumbnailUrl by remember { mutableStateOf(mediaMetadata?.thumbnailUrl) }
    var nextThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var isAnimatingTransition by remember { mutableStateOf(false) }
    var lastMediaId by remember { mutableStateOf(mediaMetadata?.id) }
    var lastQueueIndex by remember { mutableIntStateOf(playerConnection.player.currentMediaItemIndex) }
    var swipeTriggeredChange by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableStateOf(true) } 

    LaunchedEffect(mediaMetadata?.id) {
        val currentMediaId = mediaMetadata?.id
        val currentQueueIndex = playerConnection.player.currentMediaItemIndex

        if (lastMediaId != null && currentMediaId != null && lastMediaId != currentMediaId) {

            if (!swipeTriggeredChange && !isAnimatingTransition) {
                isAnimatingTransition = true

                nextThumbnailUrl = mediaMetadata?.thumbnailUrl

                animationDirection = if (currentQueueIndex > lastQueueIndex) {
                    false 
                } else if (currentQueueIndex < lastQueueIndex) {
                    true 
                } else {

                    true 
                }

                val targetX = if (animationDirection) currentView.width.toFloat() else -currentView.width.toFloat()

                coroutineScope.launch {

                    offsetXAnimatable.snapTo(0f)
                    offsetX = 0f

                    offsetXAnimatable.animateTo(
                        targetValue = targetX,
                        animationSpec = animationSpec
                    )

                    displayedThumbnailUrl = nextThumbnailUrl
                    nextThumbnailUrl = null

                    offsetXAnimatable.snapTo(0f)
                    offsetX = 0f

                    isAnimatingTransition = false
                }
            } else if (swipeTriggeredChange) {

                swipeTriggeredChange = false
            } else if (isAnimatingTransition) {

                nextThumbnailUrl = mediaMetadata?.thumbnailUrl
            }
        } else if (lastMediaId == null) {

            displayedThumbnailUrl = mediaMetadata?.thumbnailUrl
        }
        lastMediaId = currentMediaId
        lastQueueIndex = currentQueueIndex
    }

    fun animateThumbnailSwitch(direction: Boolean) { 
        if (isAnimatingTransition) return

        val nextUrl = if (direction) {
            playerConnection.player.previousMediaItemIndex.takeIf { it != -1 }?.let {
                playerConnection.player.getMediaItemAt(it).mediaMetadata.artworkUri?.toString()
            }
        } else {
            playerConnection.player.nextMediaItemIndex.takeIf { it != -1 }?.let {
                playerConnection.player.getMediaItemAt(it).mediaMetadata.artworkUri?.toString()
            }
        }

        nextThumbnailUrl = nextUrl
        animationDirection = direction
        isAnimatingTransition = true

        coroutineScope.launch {
            val targetX = if (direction) currentView.width.toFloat() else -currentView.width.toFloat()
            offsetXAnimatable.animateTo(
                targetValue = targetX,
                animationSpec = animationSpec
            )

            displayedThumbnailUrl = nextThumbnailUrl
            nextThumbnailUrl = null

            offsetXAnimatable.snapTo(0f)
            offsetX = 0f
            isAnimatingTransition = false
        }
    }

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
                    .pointerInput(swipeThumbnail) {
                        if (swipeThumbnail) {
                            detectHorizontalDragGestures(
                            onDragStart = {
                                if (isAnimatingTransition) return@detectHorizontalDragGestures
                                isPreviewingNextSong = true
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                                offsetX = offsetXAnimatable.value
                            },
                            onDragCancel = {
                                if (isAnimatingTransition) return@detectHorizontalDragGestures
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                    offsetX = 0f
                                    isPreviewingNextSong = false
                                    previewImage = null
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (isAnimatingTransition) return@detectHorizontalDragGestures
                                if (swipeThumbnail) {
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                    val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                    if (allowLeft || allowRight) {
                                        offsetX += adjustedDragAmount
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetX)
                                        }
                                        updateImagePreview(offsetX)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isAnimatingTransition) return@detectHorizontalDragGestures
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5 // 0 = 0.25, 1 = 8.5

                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                val shouldChangeSong = kotlin.math.abs(offsetX) > minDistanceThreshold && 
                                                      velocity > velocityThreshold

                                if (shouldChangeSong) {
                                    swipeTriggeredChange = true
                                    animationDirection = offsetX > 0
                                    val isRightSwipe = offsetX > 0

                                    val canSwipe = (isRightSwipe && canSkipPrevious) || (!isRightSwipe && canSkipNext)
                                    if (canSwipe) {
                                        val targetThumbnailUrl = if (isRightSwipe) {
                                            playerConnection.player.previousMediaItemIndex.takeIf { it != -1 }?.let {
                                                playerConnection.player.getMediaItemAt(it).mediaMetadata.artworkUri?.toString()
                                            }
                                        } else {
                                            playerConnection.player.nextMediaItemIndex.takeIf { it != -1 }?.let {
                                                playerConnection.player.getMediaItemAt(it).mediaMetadata.artworkUri?.toString()
                                            }
                                        }
                                        nextThumbnailUrl = targetThumbnailUrl

                                        if (isRightSwipe && canSkipPrevious) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (!isRightSwipe && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }

                                        isAnimatingTransition = true

                                        coroutineScope.launch {
                                            val targetX = if (isRightSwipe) currentView.width.toFloat() else -currentView.width.toFloat()

                                            offsetXAnimatable.animateTo(
                                                targetValue = targetX,
                                                animationSpec = animationSpec
                                            )

                                            displayedThumbnailUrl = nextThumbnailUrl
                                            nextThumbnailUrl = null

                                            offsetXAnimatable.snapTo(0f)
                                            offsetX = 0f

                                            isAnimatingTransition = false
                                            swipeTriggeredChange = false
                                        }
                                    }
                                    isPreviewingNextSong = false
                                    previewImage = null
                                } else {
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(
                                            targetValue = 0f,
                                            animationSpec = animationSpec
                                        )
                                        offsetX = 0f
                                        isPreviewingNextSong = false
                                        previewImage = null
                                    }
                                }
                            },
                        )
                        }
                    },
            ) {

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                ) {

                    AsyncImage(
                        model = displayedThumbnailUrl,
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

                    AsyncImage(
                        model = displayedThumbnailUrl,
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

                nextThumbnailUrl?.let { nextUrl ->
                    Box(
                        modifier = Modifier
                            .offset {

                                val nextThumbnailOffset = if (animationDirection) {

                                    offsetXAnimatable.value - currentView.width.toFloat()
                                } else {

                                    offsetXAnimatable.value + currentView.width.toFloat()
                                }
                                IntOffset(nextThumbnailOffset.roundToInt(), 0)
                            }
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                    ) {

                        AsyncImage(
                            model = nextUrl,
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

                        AsyncImage(
                            model = nextUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
                                    if (offsetXAnimatable.value > 0) (offsetXAnimatable.value - currentView.width).roundToInt()
                                    else (offsetXAnimatable.value + currentView.width).roundToInt(), 0
                                )
                            }
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                    ) {

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