package com.metrolist.music.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Related(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    backgroundColor: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    BottomSheet(
        state = state,
        backgroundColor = backgroundColor,
        onBackgroundColor = onBackgroundColor,
        brushBackgroundColor = Brush.verticalGradient(
            listOf(Color.Unspecified, Color.Unspecified),
        ),
        modifier = modifier,
        collapsedContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                    .clickable { state.expandSoft() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.similar),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = TextBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.related),
                        color = TextBackgroundColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                )
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.related),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues()
            ) {
                // You might also like section
                item {
                    RelatedSectionHeader(
                        title = stringResource(id = R.string.you_might_also_like),
                        onBackgroundColor = onBackgroundColor
                    )
                }

                // Placeholder for related songs
                items(5) { index ->
                    RelatedSongItem(
                        title = "Related Song ${index + 1}",
                        artist = "Artist ${index + 1}",
                        onClick = { /* TODO: Implement navigation */ },
                        onBackgroundColor = onBackgroundColor,
                        textButtonColor = textButtonColor
                    )
                }

                // Similar artists section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    RelatedSectionHeader(
                        title = stringResource(id = R.string.similar_artists),
                        onBackgroundColor = onBackgroundColor
                    )
                }

                // Placeholder for similar artists
                items(3) { index ->
                    RelatedArtistItem(
                        name = "Similar Artist ${index + 1}",
                        onClick = { /* TODO: Implement navigation */ },
                        onBackgroundColor = onBackgroundColor,
                        textButtonColor = textButtonColor
                    )
                }

                // For albums - recommended playlists section
                mediaMetadata?.album?.let {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        RelatedSectionHeader(
                            title = stringResource(id = R.string.recommended_playlists),
                            onBackgroundColor = onBackgroundColor
                        )
                    }

                    items(3) { index ->
                        RelatedPlaylistItem(
                            name = "Recommended Playlist ${index + 1}",
                            onClick = { /* TODO: Implement navigation */ },
                            onBackgroundColor = onBackgroundColor,
                            textButtonColor = textButtonColor
                        )
                    }
                }

                // More from artist section
                mediaMetadata?.artists?.firstOrNull()?.let { artist ->
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        RelatedSectionHeader(
                            title = stringResource(id = R.string.more_from_artist, artist.name),
                            onBackgroundColor = onBackgroundColor
                        )
                    }

                    items(4) { index ->
                        RelatedSongItem(
                            title = "More from ${artist.name} ${index + 1}",
                            artist = artist.name,
                            onClick = { /* TODO: Implement navigation */ },
                            onBackgroundColor = onBackgroundColor,
                            textButtonColor = textButtonColor
                        )
                    }
                }

                // About the artist section
                mediaMetadata?.artists?.firstOrNull()?.let { artist ->
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        RelatedSectionHeader(
                            title = stringResource(id = R.string.about_artist, artist.name),
                            onBackgroundColor = onBackgroundColor
                        )
                    }

                    item {
                        AboutArtistItem(
                            artistName = artist.name,
                            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                            onClick = { /* TODO: Implement navigation to artist page */ },
                            onBackgroundColor = onBackgroundColor,
                            textButtonColor = textButtonColor
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun RelatedSectionHeader(
    title: String,
    onBackgroundColor: Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = onBackgroundColor,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun RelatedSongItem(
    title: String,
    artist: String,
    onClick: () -> Unit,
    onBackgroundColor: Color,
    textButtonColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(textButtonColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = onBackgroundColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = onBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = onBackgroundColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RelatedArtistItem(
    name: String,
    onClick: () -> Unit,
    onBackgroundColor: Color,
    textButtonColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(textButtonColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.person),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = onBackgroundColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = onBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RelatedPlaylistItem(
    name: String,
    onClick: () -> Unit,
    onBackgroundColor: Color,
    textButtonColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(textButtonColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.playlist_play),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = onBackgroundColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = onBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AboutArtistItem(
    artistName: String,
    description: String,
    onClick: () -> Unit,
    onBackgroundColor: Color,
    textButtonColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = onBackgroundColor.copy(alpha = 0.8f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(id = R.string.view_artist),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}