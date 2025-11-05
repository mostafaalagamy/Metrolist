package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.EnableAutomaticOfflineModeKey
import com.metrolist.music.constants.ForceOfflineModeKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceGroupTitle
import com.metrolist.music.ui.component.SwitchPreference
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (enableAutomaticOfflineMode, onEnableAutomaticOfflineModeChange) = rememberPreference(
        key = EnableAutomaticOfflineModeKey,
        defaultValue = true
    )
    val (forceOfflineMode, onForceOfflineModeChange) = rememberPreference(
        key = ForceOfflineModeKey,
        defaultValue = false
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = "Offline Mode")

        SwitchPreference(
            title = { Text("Enable Automatic Offline Mode") },
            description = "Automatically switch to offline mode when no internet connection is detected.",
            icon = { Icon(painterResource(R.drawable.offline), null) },
            checked = enableAutomaticOfflineMode,
            onCheckedChange = onEnableAutomaticOfflineModeChange,
        )

        SwitchPreference(
            title = { Text("Force Offline Mode") },
            description = "Only show downloaded content, regardless of internet connection.",
            icon = { Icon(painterResource(R.drawable.security), null) },
            checked = forceOfflineMode,
            onCheckedChange = onForceOfflineModeChange,
        )
    }

    TopAppBar(
        title = { Text("Library") },
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
        scrollBehavior = scrollBehavior
    )
}
