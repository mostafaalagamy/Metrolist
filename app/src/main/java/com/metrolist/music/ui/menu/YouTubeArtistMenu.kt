package com.metrolist.music.ui.menu

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.YouTubeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeArtistMenu(
    artist: ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist(artist.id).collectAsState(initial = null)

    YouTubeListItem(
        item = artist,
        trailingContent = {
            IconButton(
                onClick = {
                    database.query {
                        val libraryArtist = libraryArtist
                        if (libraryArtist != null) {
                            update(libraryArtist.artist.toggleLike())
                        } else {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    channelId = artist.channelId,
                                    thumbnailUrl = artist.thumbnail,
                                ).toggleLike()
                            )
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(
                        if (libraryArtist?.artist?.bookmarkedAt != null) {
                            R.drawable.favorite
                        } else {
                            R.drawable.favorite_border
                        },
                    ),
                    tint = if (libraryArtist?.artist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
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
        artist.radioEndpoint?.let { watchEndpoint ->
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.start_radio)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.radio),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                        onDismiss()
                    }
                )
            }
        }
        artist.shuffleEndpoint?.let { watchEndpoint ->
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
                        playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                        onDismiss()
                    }
                )
            }
        }
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
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
            )
        }
    }
}
