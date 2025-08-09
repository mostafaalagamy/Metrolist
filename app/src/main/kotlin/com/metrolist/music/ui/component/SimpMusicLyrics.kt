package com.metrolist.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.extensions.animateScrollAndCentralizeItem
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpMusicLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }
    
    // Parse lyrics similar to SimpMusic approach
    val lines = remember(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            emptyList()
        } else {
            LyricsUtils.parseLyrics(lyrics)
        }
    }

    var currentLineIndex by rememberSaveable { mutableIntStateOf(-1) }
    var currentLineHeight by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val current by playerConnection.playbackState.collectAsState()

    // SimpMusic-inspired line detection logic
    LaunchedEffect(key1 = current) {
        if (lines.isEmpty()) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        
        val currentTimeMs = sliderPositionProvider() ?: playerConnection.player.currentPosition
        
        if (currentTimeMs > 0L) {
            lines.indices.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTime
                
                // Estimate end time based on next sentence or add default duration
                val endTimeMs = if (i < lines.size - 1) {
                    lines[i + 1].startTime
                } else {
                    startTimeMs + 60000 // Default 1 minute for last line
                }
                
                if (currentTimeMs in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                    return@LaunchedEffect
                }
            }
            
            // If before first line, set to -1
            if (lines.isNotEmpty() && currentTimeMs < lines[0].startTime) {
                currentLineIndex = -1
            }
        } else {
            currentLineIndex = -1
        }
    }

    // SimpMusic-inspired smooth animation
    LaunchedEffect(key1 = currentLineIndex, key2 = currentLineHeight) {
        if (currentLineIndex > -1 && currentLineHeight > 0 && lines.isNotEmpty()) {
            listState.animateScrollAndCentralizeItem(
                index = currentLineIndex,
                scope = this
            )
        }
    }

    if (lines.isEmpty()) {
        // Show no lyrics message
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        // SimpMusic-style lyrics display
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Add top spacing
            item {
                Box(modifier = Modifier.height(200.dp))
            }
            
            itemsIndexed(lines) { index, line ->
                val isCurrentLine = index == currentLineIndex
                val alpha = when {
                    isCurrentLine -> 1f
                    index == currentLineIndex - 1 || index == currentLineIndex + 1 -> 0.6f
                    else -> 0.4f
                }
                
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isCurrentLine) 24.sp else 20.sp,
                        fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = textColor.copy(alpha = alpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                        .onGloballyPositioned { layoutCoordinates ->
                            if (isCurrentLine) {
                                currentLineHeight = layoutCoordinates.size.height
                            }
                        }
                )
            }
            
            // Add bottom spacing
            item {
                Box(modifier = Modifier.height(200.dp))
            }
        }
    }
}