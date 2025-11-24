package com.metrolist.music.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItem
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
        trailingContent = {},
    )

    HorizontalDivider()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                bottom = 8.dp + WindowInsets.systemBars
                    .asPaddingValues()
                    .calculateBottomPadding()
            )
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        val actions = buildList {
            if (artist.songCount > 0) {
                add(
                    Material3MenuItem(
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        title = { Text(stringResource(R.string.play)) },
                        onClick = {
                            coroutineScope.launch {
                                val songs = withContext(Dispatchers.IO) {
                                    database
                                        .artistSongs(
                                            artist.id,
                                            ArtistSongSortType.CREATE_DATE,
                                            true
                                        )
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
                )

                add(
                    Material3MenuItem(
                        icon = { Icon(painterResource(R.drawable.shuffle), null) },
                        title = { Text(stringResource(R.string.shuffle)) },
                        onClick = {
                            coroutineScope.launch {
                                val songs = withContext(Dispatchers.IO) {
                                    database
                                        .artistSongs(
                                            artist.id,
                                            ArtistSongSortType.CREATE_DATE,
                                            true
                                        )
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
                )
            }

            if (artist.artist.isYouTubeArtist) {
                add(
                    Material3MenuItem(
                        icon = { Icon(painterResource(R.drawable.share), null) },
                        title = { Text(stringResource(R.string.share)) },
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
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
                )
            }
        }
        if (actions.isNotEmpty()) {
            Material3MenuGroup(items = actions)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Material3MenuGroup(
            items = listOf(
                Material3MenuItem(
                    title = {
                        Text(text = if (artist.artist.bookmarkedAt != null) stringResource(R.string.subscribed) else stringResource(R.string.subscribe))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.subscribed else R.drawable.subscribe),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        database.transaction {
                            update(artist.artist.toggleLike())
                        }
                    }
                )
            )
        )
    }
}
