package com.metrolist.music.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MaxImageCacheSizeKey
import com.metrolist.music.constants.MaxSongCacheSizeKey
import com.metrolist.music.extensions.tryOrNull
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ListPreference
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.PreferenceGroupTitle
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.formatFileSize
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(tryOrNull { playerCache.cacheSpace } ?: 0)
    }
    var downloadCacheSize by remember {
        mutableStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0)
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue = (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f),
        label = "imageCacheProgress",
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(
            0f,
            1f
        ),
        label = "playerCacheProgress",
    )

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(500)
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.downloaded_songs),
        )

        Text(
            text = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_all_downloads)) },
            onClick = {clearDownloads = true
            },
        )

        if (clearDownloads) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_all_downloads),
                onDismiss = { clearDownloads = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        downloadCache.keys.forEach { key ->
                            downloadCache.removeResource(key)
                        }
                    }
                    clearDownloads = false
                },
                onCancel = { clearDownloads = false },
                content = {
                    Text(text = stringResource(R.string.clear_downloads_dialog))
                }
            )
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.song_cache),
        )

        if (maxSongCacheSize != 0) {
            if (maxSongCacheSize == -1) {
                Text(
                    text = stringResource(R.string.size_used, formatFileSize(playerCacheSize)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            } else {
                // Use M3 LinearProgressIndicator with theme colors
                LinearProgressIndicator(
                    progress = { playerCacheProgress },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary, // Explicitly use theme color
                    trackColor = MaterialTheme.colorScheme.surfaceVariant, // Use appropriate track color
                    strokeCap = StrokeCap.Round // M3 default style
                )

                Text(
                    text =
                    stringResource(
                        R.string.size_used,
                        "${formatFileSize(playerCacheSize)} / ${
                            formatFileSize(
                                maxSongCacheSize * 1024 * 1024L,
                            )
                        }",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        ListPreference(
            title = { Text(stringResource(R.string.max_cache_size)) },
            selectedValue = maxSongCacheSize,
            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
            valueText = {
                when (it) {
                    0 -> stringResource(R.string.disable)
                    -1 -> stringResource(R.string.unlimited)
                    else -> formatFileSize(it * 1024 * 1024L)
                }
            },
            onValueSelected = onMaxSongCacheSizeChange,
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_song_cache)) },
            onClick = { clearCacheDialog = true
            },
        )

        if (clearCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_song_cache),
                onDismiss = { clearCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
                        }
                    }
                    clearCacheDialog = false
                },
                onCancel = { clearCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_song_cache_dialog))
                }
            )
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.image_cache),
        )

        if (maxImageCacheSize > 0) {
            // Use M3 LinearProgressIndicator with theme colors
            LinearProgressIndicator(
                progress = { imageCacheProgress },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary, // Explicitly use theme color
                trackColor = MaterialTheme.colorScheme.surfaceVariant, // Use appropriate track color
                strokeCap = StrokeCap.Round // M3 default style
            )

            Text(
                text = stringResource(
                    R.string.size_used,
                    "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        ListPreference(
            title = { Text(stringResource(R.string.max_cache_size)) },
            selectedValue = maxImageCacheSize,
            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
            valueText = {
                when (it) {
                    0 -> stringResource(R.string.disable)
                    else -> formatFileSize(it * 1024 * 1024L)
                }
            },
            onValueSelected = onMaxImageCacheSizeChange,
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_image_cache)) },
            onClick = { clearImageCacheDialog = true
            },
        )

        if (clearImageCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_image_cache),
                onDismiss = { clearImageCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        imageDiskCache.clear()
                    }
                    clearImageCacheDialog = false
                },
                onCancel = { clearImageCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_image_cache_dialog))
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
