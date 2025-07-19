package com.metrolist.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ArrowBack
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.ReleaseNotesCard
import com.metrolist.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.appearance)) },
            icon = { Icon(Icons.Filled.Palette, null) },
            onClick = { navController.navigate("settings/appearance") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.content)) },
            icon = { Icon(Icons.Filled.Language, null) },
            onClick = { navController.navigate("settings/content") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.privacy)) },
            icon = { Icon(Icons.Filled.Security, null) },
            onClick = { navController.navigate("settings/privacy") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.player_and_audio)) },
            icon = { Icon(Icons.Filled.PlayArrow, null) },
            onClick = { navController.navigate("settings/player") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.storage)) },
            icon = { Icon(Icons.Filled.Storage, null) },
            onClick = { navController.navigate("settings/storage") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.backup_restore)) },
            icon = { Icon(Icons.Filled.Restore, null) },
            onClick = { navController.navigate("settings/backup_restore") }
        )
        if (isAndroid12OrLater) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.default_links)) },
                icon = { Icon(Icons.Filled.Link, null) },
                onClick = {
                    try {
                        val intent = Intent(
                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        when (e) {
                            is ActivityNotFoundException -> {
                                Toast.makeText(
                                    context,
                                    R.string.open_app_settings_error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is SecurityException -> {
                                Toast.makeText(
                                    context,
                                    R.string.open_app_settings_error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> {
                                Toast.makeText(
                                    context,
                                    R.string.open_app_settings_error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
            )
        }
        PreferenceEntry(
            title = { Text(stringResource(R.string.about)) },
            icon = { Icon(Icons.Filled.Info, null) },
            onClick = { navController.navigate("settings/about") }
        )
        if (latestVersionName != BuildConfig.VERSION_NAME) {
            PreferenceEntry(
                title = {
                    Text(
                        text = stringResource(R.string.new_version_available),
                    )
                },
                description = latestVersionName,
                icon = {
                    BadgedBox(
                        badge = { Badge() }
                    ) {
                        Icon(Icons.Filled.Update, null)
                    }
                },
                onClick = {
                    uriHandler.openUri("https://github.com/mostafaalagamy/Metrolist/releases/latest/download/Metrolist.apk")
                }
            )
            ReleaseNotesCard()
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}
