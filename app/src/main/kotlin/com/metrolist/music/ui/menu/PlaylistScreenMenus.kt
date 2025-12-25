package com.metrolist.music.ui.menu

import android.content.Context
import android.content.Intent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.metrolist.music.R
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData

/**
 * Menu for Local Playlist Screen
 */
@Composable
fun LocalPlaylistMenu(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    context: Context,
    onEdit: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    Material3MenuGroup(
        items = listOf(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.edit)) },
                description = { Text(stringResource(R.string.edit_playlist)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null
                    )
                },
                onClick = {
                    onEdit()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.action_sync)) },
                description = { Text(stringResource(R.string.sync_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = null
                    )
                },
                onClick = {
                    onSync()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.add_to_queue)) },
                description = { Text(stringResource(R.string.add_to_queue_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null
                    )
                },
                onClick = {
                    onQueue()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.action_download)) },
                description = { Text(stringResource(R.string.download_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null
                    )
                },
                onClick = {
                    onDownload()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.share)) },
                description = { Text(stringResource(R.string.share_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null
                    )
                },
                onClick = {
                    val shareText = if (playlist.playlist.browseId != null) {
                        "https://music.youtube.com/playlist?list=${playlist.playlist.browseId}"
                    } else {
                        songs.joinToString("\n") { it.song.song.title }
                    }
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.delete)) },
                description = { Text(stringResource(R.string.delete_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null
                    )
                },
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
        )
    )
}

/**
 * Menu for Auto Playlist Screen (Liked Songs, Downloaded Songs, etc.)
 */
@Composable
fun AutoPlaylistMenu(
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Material3MenuGroup(
        items = listOf(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.add_to_queue)) },
                description = { Text(stringResource(R.string.add_to_queue_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null
                    )
                },
                onClick = {
                    onQueue()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.action_download)) },
                description = { Text(stringResource(R.string.download_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null
                    )
                },
                onClick = {
                    onDownload()
                    onDismiss()
                }
            )
        )
    )
}

/**
 * Menu for Top Playlist Screen
 */
@Composable
fun TopPlaylistMenu(
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Material3MenuGroup(
        items = listOf(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.add_to_queue)) },
                description = { Text(stringResource(R.string.add_to_queue_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null
                    )
                },
                onClick = {
                    onQueue()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.action_download)) },
                description = { Text(stringResource(R.string.download_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null
                    )
                },
                onClick = {
                    onDownload()
                    onDismiss()
                }
            )
        )
    )
}

/**
 * Menu for Cache Playlist Screen
 */
@Composable
fun CachePlaylistMenu(
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Material3MenuGroup(
        items = listOf(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.add_to_queue)) },
                description = { Text(stringResource(R.string.add_to_queue_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null
                    )
                },
                onClick = {
                    onQueue()
                    onDismiss()
                }
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.action_download)) },
                description = { Text(stringResource(R.string.download_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null
                    )
                },
                onClick = {
                    onDownload()
                    onDismiss()
                }
            )
        )
    )
}
