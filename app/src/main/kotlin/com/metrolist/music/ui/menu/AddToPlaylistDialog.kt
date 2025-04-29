package com.metrolist.music.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend (Playlist) -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var playlists by remember {
        mutableStateOf(emptyList<Playlist>())
    }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var showCreatePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }
    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }
    var songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }
    var duplicates by remember {
        mutableStateOf(emptyList<String>())
    }

    LaunchedEffect(Unit) {
        database.editablePlaylistsByCreateDateAsc().collect {
            playlists = it.asReversed()
        }
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss,
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.create_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            items(playlists) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier.clickable {
                        selectedPlaylist = playlist
                        coroutineScope.launch(Dispatchers.IO) {
                            if (songIds == null) {
                                songIds = onGetSong(playlist)
                            }
                            duplicates = database.playlistDuplicates(playlist.id, songIds!!)
                            if (duplicates.isNotEmpty()) {
                                showDuplicateDialog = true
                            } else {
                                onDismiss()
                                database.addSongToPlaylist(playlist, songIds!!)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    // duplicate songs warning
        if (showDuplicateDialog) {
            DefaultDialog(
                title = { Text(stringResource(R.string.duplicates)) },
                buttons = {
                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            onDismiss()
                            database.transaction {
                                addSongToPlaylist(
                                    selectedPlaylist!!,
                                    songIds!!.filter {
                                        !duplicates.contains(it)
                                    }
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.skip_duplicates))
                    }

                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            onDismiss()
                            database.transaction {
                                addSongToPlaylist(selectedPlaylist!!, songIds!!)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add_anyway))
                    }

                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                onDismiss = {
                    showDuplicateDialog = false
                }
            ) {
                Text(
                    text = if (duplicates.size == 1) {
                        stringResource(R.string.duplicates_description_single)
                    } else {
                        stringResource(R.string.duplicates_description_multiple, duplicates.size)
                    },
                    textAlign = TextAlign.Start,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
}
