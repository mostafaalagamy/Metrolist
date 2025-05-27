package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsEntry.Companion.HEAD_LYRICS_ENTRY
import com.metrolist.music.lyrics.LyricsUtils.findCurrentLineIndex
import com.metrolist.music.lyrics.LyricsUtils.parseLyrics
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.LyricsMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.utils.ComposeToImage
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds


@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current // Get configuration

    val landscapeOffset =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lines =
        remember(lyrics) {
            if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                emptyList()
            } else if (lyrics.startsWith("[")) {
                listOf(HEAD_LYRICS_ENTRY) + parseLyrics(lyrics)
            } else {
                lyrics.lines().mapIndexed { index, line -> LyricsEntry(index * 100L, line) }
            }
        }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var previousLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var initialScrollDone by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldScrollToFirstLine by rememberSaveable {
        mutableStateOf(true)
    }

    var isAppMinimized by rememberSaveable {
        mutableStateOf(false)
    }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    // State for multi-selection
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) } // State for showing max selection toast

    val lazyListState = rememberLazyListState()

    // Define max selection limit
    val maxSelectionLimit = 5

    // Show toast when max selection is reached
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if(event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset selection mode if lyrics change
    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            currentLineIndex = findCurrentLineIndex(
                lines,
                sliderPosition ?: playerConnection.player.currentPosition
            )
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone) {
        /**
         * Count number of new lines in a lyric
         */
        fun countNewLine(str: String) = str.count { it == '\n' }

        /**
         * Calculate the lyric offset Based on how many lines (\n chars)
         */
        fun calculateOffset() = with(density) {
            if (currentLineIndex < 0 || currentLineIndex >= lines.size) return@with 0
            val count = countNewLine(lines[currentLineIndex].text)
            val dpValue = if (landscapeOffset) 16.dp else 20.dp
            dpValue.toPx().toInt() * count
        }

        if (!isSynced) return@LaunchedEffect
        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            lazyListState.scrollToItem(
                currentLineIndex,
                with(density) { 36.dp.toPx().toInt() } + calculateOffset())
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                lazyListState.scrollToItem(
                    currentLineIndex,
                    with(density) { 36.dp.toPx().toInt() } + calculateOffset())
            } else if (lastPreviewTime == 0L || currentLineIndex != previousLineIndex) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }

                if (isCurrentLineVisible) {
                    val viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset
                    val viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset
                    val currentLineOffset = visibleItemsInfo.find { it.index == currentLineIndex }?.offset ?: 0
                    val previousLineOffset = visibleItemsInfo.find { it.index == previousLineIndex }?.offset ?: 0

                    val centerRangeStart = viewportStartOffset + (viewportEndOffset - viewportStartOffset) / 2
                    val centerRangeEnd = viewportEndOffset - (viewportEndOffset - viewportStartOffset) / 8

                    if (currentLineOffset in centerRangeStart..centerRangeEnd ||
                        previousLineOffset in centerRangeStart..centerRangeEnd) {
                        lazyListState.animateScrollToItem(
                            currentLineIndex,
                            with(density) { 36.dp.toPx().toInt() } + calculateOffset())
                    }
                }
            }
        }
        if(currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Top)
                .add(WindowInsets(top = maxHeight / 2, bottom = maxHeight / 2))
                .asPaddingValues(),
            modifier = Modifier
                .fadingEdge(vertical = 64.dp)
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (!isSelectionModeActive) { // Only update preview time if not selecting
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            if (!isSelectionModeActive) { // Only update preview time if not selecting
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
        ) {
            val displayedCurrentLineIndex =
                if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex

            if (lyrics == null) {
                item {
                    ShimmerHost {
                        repeat(10) {
                            Box(
                                contentAlignment = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Alignment.CenterStart
                                    LyricsPosition.CENTER -> Alignment.Center
                                    LyricsPosition.RIGHT -> Alignment.CenterEnd
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                            ) {
                                TextPlaceholder()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = lines,
                    key = { index, item -> "$index-${item.time}" } // Add stable key
                ) { index, item ->
                    val isSelected = selectedIndices.contains(index)
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // Clip for background
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    // Toggle selection
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive = false // Exit mode if last item deselected
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics) {
                                    // Seek action
                                    playerConnection.player.seekTo(item.time)
                                    scope.launch {
                                        lazyListState.animateScrollToItem(
                                            index,
                                            with(density) { 36.dp.toPx().toInt() } +
                                                    with(density) {
                                                        val count = item.text.count { it == '\n' }
                                                        (if (landscapeOffset) 16.dp.toPx() else 20.dp.toPx()).toInt() * count
                                                    }
                                        )
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    // If already in selection mode and item not selected, add it if below limit
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    // If already at limit, show toast
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        .background(
                            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .alpha(
                            if (!isSynced || index == displayedCurrentLineIndex || (isSelectionModeActive && isSelected)) 1f
                            else 0.5f
                        )

                    Text(
                        text = item.text,
                        fontSize = 20.sp,
                        color = textColor,
                        textAlign = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = itemModifier
                    )
                }
            }
        }

        if (lyrics == LYRICS_NOT_FOUND) {
            Text(
                text = stringResource(R.string.lyrics_not_found),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> TextAlign.Left
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.Right
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .alpha(0.5f)
            )
        }

        mediaMetadata?.let { metadata ->
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionModeActive) {
                    // Cancel Selection Button
                    IconButton(
                        onClick = {
                            isSelectionModeActive = false
                            selectedIndices.clear()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.cancel),
                            tint = textColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Share Selected Button
                    IconButton(
                        onClick = {
                            if (selectedIndices.isNotEmpty()) {
                                val sortedIndices = selectedIndices.sorted()
                                val selectedLyricsText = sortedIndices
                                    .mapNotNull { lines.getOrNull(it)?.text }
                                    .joinToString("\n")

                                if (selectedLyricsText.isNotBlank()) {
                                    shareDialogData = Triple(
                                        selectedLyricsText,
                                        metadata.title, // Provide default empty string
                                        metadata.artists.joinToString { it.name }
                                    )
                                    showShareDialog = true
                                }
                                isSelectionModeActive = false
                                selectedIndices.clear()
                            }
                        },
                        enabled = selectedIndices.isNotEmpty() // Disable if nothing selected
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.media3_icon_share),
                            contentDescription = stringResource(R.string.share_selected),
                            tint = if (selectedIndices.isNotEmpty()) textColor else textColor.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    // Original More Button
                    IconButton(
                        onClick = {
                            menuState.show {
                                LyricsMenu(
                                    lyricsProvider = { lyricsEntity },
                                    mediaMetadataProvider = { metadata },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more_horiz),
                            contentDescription = stringResource(R.string.more_options),
                            tint = textColor
                        )
                    }
                }
            }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* Don't dismiss */ }) {
            Card( // Use Card for better styling
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! // Renamed 'lyrics' to 'lyricsText' for clarity
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DividerDefaults.color) // Use default color
                    // Share as Text Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    // Use the potentially multi-line lyricsText here
                                    putExtra(Intent.EXTRA_TEXT, "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.media3_icon_share), // Consistent share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = DividerDefaults.color)
                    // Share as Image Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Pass the potentially multi-line lyrics to the color picker
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showColorPickerDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            // Changed icon to represent image sharing better
                            painter = painterResource(id = R.drawable.media3_icon_share), // Use a relevant icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = DividerDefaults.color)
                    // Cancel Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium // Make cancel slightly bolder
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = null, // Description is handled by Text
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl
        val paletteColors = remember { mutableStateListOf<Color>() }

        val previewCardWidth = configuration.screenWidthDp.dp * 0.90f
        val previewPadding = 20.dp * 2
        val previewBoxPadding = 28.dp * 2
        val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
        val previewBoxHeight = 340.dp
        val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
        val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

        val textStyleForMeasurement = TextStyle(
            color = previewTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        val textMeasurer = rememberTextMeasurer()

        val calculatedFontSize = rememberAdjustedFontSize(
            text = lyricsText,
            maxWidth = previewAvailableWidth,
            maxHeight = previewAvailableHeight,
            density = density,
            initialFontSize = 50.sp,
            minFontSize = 22.sp,
            style = textStyleForMeasurement,
            textMeasurer = textMeasurer
        )

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val result = loader.execute(req)
                        val bmp = result.drawable?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            val swatches = palette.swatches.sortedByDescending { it.population }
                            val colors = swatches.map { Color(it.rgb) }
                                .filter { color ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hsv[1] > 0.2f
                                }
                            paletteColors.clear()
                            paletteColors.addAll(colors.take(5))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(8.dp)
                    ) {
                        LyricsImageCard(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            backgroundColor = previewBackgroundColor,
                            textColor = previewTextColor,
                            secondaryTextColor = previewSecondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(text = stringResource(id = R.string.background_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewBackgroundColor = color }
                                    .border(2.dp, if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color.White, Color.Black, Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewTextColor = color }
                                    .border(2.dp, if (previewTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewSecondaryTextColor = color }
                                    .border(2.dp, if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val screenWidth = configuration.screenWidthDp
                                    val screenHeight = configuration.screenHeightDp

                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = (screenWidth * density.density).toInt(),
                                        height = (screenHeight * density.density).toInt(),
                                        backgroundColor = previewBackgroundColor.toArgb(),
                                        textColor = previewTextColor.toArgb(),
                                        secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                    )
                                    val timestamp = System.currentTimeMillis()
                                    val filename = "lyrics_$timestamp"
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to create image: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    showProgressDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                }
            }
        }
    }
}

// Constants remain unchanged
const val ANIMATE_SCROLL_DURATION = 300L
val LyricsPreviewTime = 2.seconds
