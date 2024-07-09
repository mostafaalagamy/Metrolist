package com.malopieds.innertune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.malopieds.innertune.LocalDatabase
import com.malopieds.innertune.LocalPlayerAwareWindowInsets
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.EnableKugouKey
import com.malopieds.innertune.constants.EnableLrcLibKey
import com.malopieds.innertune.constants.PauseListenHistoryKey
import com.malopieds.innertune.constants.PauseSearchHistoryKey
import com.malopieds.innertune.constants.PreferredLyricsProvider
import com.malopieds.innertune.constants.PreferredLyricsProviderKey
import com.malopieds.innertune.ui.component.DefaultDialog
import com.malopieds.innertune.ui.component.IconButton
import com.malopieds.innertune.ui.component.ListPreference
import com.malopieds.innertune.ui.component.PreferenceEntry
import com.malopieds.innertune.ui.component.SwitchPreference
import com.malopieds.innertune.ui.utils.backToMain
import com.malopieds.innertune.utils.rememberEnumPreference
import com.malopieds.innertune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val database = LocalDatabase.current
    val (pauseListenHistory, onPauseListenHistoryChange) = rememberPreference(key = PauseListenHistoryKey, defaultValue = false)
    val (pauseSearchHistory, onPauseSearchHistoryChange) = rememberPreference(key = PauseSearchHistoryKey, defaultValue = false)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (preferredProvider, onPreferredProviderChange) =
        rememberEnumPreference(
            key = PreferredLyricsProviderKey,
            defaultValue = PreferredLyricsProvider.LRCLIB,
        )

    var showClearListenHistoryDialog by remember {
        mutableStateOf(false)
    }

    if (showClearListenHistoryDialog) {
        DefaultDialog(
            onDismiss = { showClearListenHistoryDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_listen_history_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showClearListenHistoryDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearListenHistoryDialog = false
                        database.query {
                            clearListenHistory()
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showClearSearchHistoryDialog by remember {
        mutableStateOf(false)
    }

    if (showClearSearchHistoryDialog) {
        DefaultDialog(
            onDismiss = { showClearSearchHistoryDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_search_history_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showClearSearchHistoryDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearSearchHistoryDialog = false
                        database.query {
                            clearSearchHistory()
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        SwitchPreference(
            title = { Text(stringResource(R.string.pause_listen_history)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            checked = pauseListenHistory,
            onCheckedChange = onPauseListenHistoryChange,
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_listen_history)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            onClick = { showClearListenHistoryDialog = true },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.pause_search_history)) },
            icon = { Icon(painterResource(R.drawable.manage_search), null) },
            checked = pauseSearchHistory,
            onCheckedChange = onPauseSearchHistoryChange,
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_search_history)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            onClick = { showClearSearchHistoryDialog = true },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableLrclib,
            onCheckedChange = onEnableLrclibChange,
        )

        ListPreference(
            title = { Text(stringResource(R.string.set_quick_picks)) },
            selectedValue = preferredProvider,
            values = listOf(PreferredLyricsProvider.KUGOU, PreferredLyricsProvider.LRCLIB),
            valueText = { it.name.toLowerCase(Locale.current).capitalize(Locale.current) },
            onValueSelected = onPreferredProviderChange,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.privacy)) },
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
        },
    )
}
