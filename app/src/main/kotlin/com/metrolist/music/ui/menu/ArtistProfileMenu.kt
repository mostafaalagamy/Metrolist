package com.metrolist.music.ui.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItem
import com.metrolist.music.viewmodels.ArtistViewModel

@Composable
fun ArtistProfileMenu(
    navController: NavController,
    viewModel: ArtistViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()

    LazyColumn(
        contentPadding =
        PaddingValues(top = 12.dp, bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3MenuGroup(
                items = listOf(
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
                                    "https://music.youtube.com/channel/${viewModel.artistId}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    ),
                    Material3MenuItem(
                        icon = { Icon(painterResource(R.drawable.link), null) },
                        title = { Text(stringResource(R.string.copy_link)) },
                        onClick = {
                            onDismiss()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Artist Link", "https://music.youtube.com/channel/${viewModel.artistId}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            )
        }
        item {
            Material3MenuGroup(
                items = listOf(
                    Material3MenuItem(
                        title = {
                            Text(text = if (libraryArtist?.artist?.bookmarkedAt != null) stringResource(R.string.subscribed) else stringResource(R.string.subscribe))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(if (libraryArtist?.artist?.bookmarkedAt != null) R.drawable.subscribed else R.drawable.subscribe),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            database.transaction {
                                val artist = libraryArtist?.artist
                                if (artist != null) {
                                    update(artist.toggleLike())
                                } else {
                                    artistPage?.artist?.let {
                                        insert(
                                            ArtistEntity(
                                                id = it.id,
                                                name = it.title,
                                                channelId = it.channelId,
                                                thumbnailUrl = it.thumbnail,
                                            ).toggleLike()
                                        )
                                    }
                                }
                            }
                            onDismiss()
                        }
                    )
                )
            )
        }
    }
}
