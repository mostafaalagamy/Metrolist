package com.metrolist.music.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.utils.rememberPreference
import com.metrolist.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import kotlinx.coroutines.withContext

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
    var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var playlistsWithDuplicates by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var duplicateSongsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var songIds by remember { mutableStateOf<List<String>?>(null) }
    val (selectedPlaylistIds, setSelectedPlaylistIds) = remember { mutableStateOf(emptySet<String>()) }


    LaunchedEffect(Unit) {
        database.editablePlaylistsByCreateDateAsc().collect {
            playlists = it.asReversed()
        }
    }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to playlist") },
            text = {
                LazyColumn {
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
                            modifier = Modifier.clickable { showCreatePlaylistDialog = true }
                        )
                    }
                    items(playlists) { playlist ->
                        val isSelected = selectedPlaylistIds.contains(playlist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentIds = selectedPlaylistIds.toMutableSet()
                                    if (isSelected) currentIds.remove(playlist.id)
                                    else currentIds.add(playlist.id)
                                    setSelectedPlaylistIds(currentIds)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistListItem(
                                playlist = playlist,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { isChecked ->
                                    val currentIds = selectedPlaylistIds.toMutableSet()
                                    if (isChecked) currentIds.add(playlist.id)
                                    else currentIds.remove(playlist.id)
                                    setSelectedPlaylistIds(currentIds)
                                }
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedPlaylistIds.isNotEmpty(),
                    onClick = {
                        coroutineScope.launch {
                            val currentSongIds = withContext(Dispatchers.IO) {
                                songIds ?: if (playlists.isNotEmpty()) onGetSong(playlists.first()) else null
                            }

                            if (currentSongIds.isNullOrEmpty()) {
                                onDismiss()
                                return@launch
                            }
                            songIds = currentSongIds

                            val (withDuplicates, duplicatesMap) = withContext(Dispatchers.IO) {
                                val selectedPlaylists = playlists.filter { selectedPlaylistIds.contains(it.id) }
                                val tempDuplicatesMap = mutableMapOf<String, List<String>>()

                                val (playlistsWithDups, playlistsWithoutDups) = selectedPlaylists.partition { playlist ->
                                    val dups = database.playlistDuplicates(playlist.id, currentSongIds)
                                    if (dups.isNotEmpty()) {
                                        tempDuplicatesMap[playlist.id] = dups
                                        true
                                    } else {
                                        false
                                    }
                                }

                                playlistsWithoutDups.forEach { playlist ->
                                    database.addSongToPlaylist(playlist, currentSongIds)
                                    playlist.playlist.browseId?.let { plist ->
                                        currentSongIds.forEach { songId ->
                                            YouTube.addToPlaylist(plist, songId)
                                        }
                                    }
                                }
                                Pair(playlistsWithDups, tempDuplicatesMap)
                            }

                            if (withDuplicates.isNotEmpty()) {
                                playlistsWithDuplicates = withDuplicates
                                duplicateSongsMap = duplicatesMap
                                showDuplicateDialog = true
                            }
                            onDismiss()
                        }
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    //  Duplicate songs
    if (showDuplicateDialog) {
        val totalDuplicates = duplicateSongsMap.values.flatten().distinct().size
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            playlistsWithDuplicates.forEach { playlist ->
                                val duplicatesForThisPlaylist = duplicateSongsMap[playlist.id] ?: emptyList()
                                val songsToAdd = songIds!!.filter { it !in duplicatesForThisPlaylist }
                                if (songsToAdd.isNotEmpty()) {
                                    database.addSongToPlaylist(playlist, songsToAdd)
                                }
                            }
                        }
                        showDuplicateDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.skip_duplicates))
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            playlistsWithDuplicates.forEach { playlist ->
                                database.addSongToPlaylist(playlist, songIds!!)
                            }
                        }
                        showDuplicateDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.add_anyway))
                }

                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showDuplicateDialog = false }
        ) {
            Text(
                text = if (totalDuplicates == 1) {
                    stringResource(R.string.duplicates_description_single)
                } else {
                    stringResource(R.string.duplicates_description_multiple, totalDuplicates)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
