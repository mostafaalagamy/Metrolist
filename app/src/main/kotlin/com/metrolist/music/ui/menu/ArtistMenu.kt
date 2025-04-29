package com.metrolist.music.ui.menu

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.ArtistListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    ArtistListItem(
        artist = artist,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (artist.artist.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    HorizontalDivider()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        if (artist.songCount > 0) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.play)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val songs =
                                withContext(Dispatchers.IO) {
                                    database
                                        .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                        .first()
                                        .map { it.toMediaItem() }
                                }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs,
                                ),
                            )
                        }
                        onDismiss()
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.shuffle)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val songs =
                                withContext(Dispatchers.IO) {
                                    database
                                        .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                        .first()
                                        .map { it.toMediaItem() }
                                        .shuffled()
                                }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs,
                                ),
                            )
                        }
                        onDismiss()
                    }
                )
            }
        }
        if (artist.artist.isYouTubeArtist) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.share)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/channel/${artist.id}"
                                )
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                )
            }
        }
    }
}
