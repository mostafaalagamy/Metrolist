/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.PreferenceGroupTitle
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.metrolist.lastfm.LastFM
import com.metrolist.music.constants.EnableLastFMScrobblingKey
import com.metrolist.music.constants.LastFMSessionKey
import com.metrolist.music.constants.LastFMUseNowPlaying
import com.metrolist.music.constants.LastFMUseSendLikes
import com.metrolist.music.constants.LastFMUsernameKey
import com.metrolist.music.constants.ScrobbleMinSongDurationKey
import com.metrolist.music.constants.ScrobbleDelayPercentKey
import com.metrolist.music.constants.ScrobbleDelaySecondsKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.SwitchPreference
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.reportException
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFMSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) { 
    val coroutineScope = rememberCoroutineScope()

    var lastfmUsername by rememberPreference(LastFMUsernameKey, "")
    var lastfmSession by rememberPreference(LastFMSessionKey, "")

    val isLoggedIn =
        remember(lastfmSession) {
            lastfmSession != ""
        }

    val (useNowPlaying, onUseNowPlayingChange) = rememberPreference(
        key = LastFMUseNowPlaying,
        defaultValue = false
    )

    val (useSendLikes, onUseSendLikes) = rememberPreference(
        key = LastFMUseSendLikes,
        defaultValue = false
    )

    val (lastfmScrobbling, onlastfmScrobblingChange) = rememberPreference(
        key = EnableLastFMScrobblingKey,
        defaultValue = false
    )

    val (scrobbleDelayPercent, onScrobbleDelayPercentChange) = rememberPreference(
        ScrobbleDelayPercentKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT
    )

    val (minTrackDuration, onMinTrackDurationChange) = rememberPreference(
        ScrobbleMinSongDurationKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION
    )

    val (scrobbleDelaySeconds, onScrobbleDelaySecondsChange) = rememberPreference(
        ScrobbleDelaySecondsKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
    )

    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }

    if (showLoginDialog) {
        var tempUsername by rememberSaveable { mutableStateOf("") }
        var tempPassword by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { 
                if (!isLoggingIn) {
                    showLoginDialog = false
                    loginError = null
                }
            },
            title = { Text(stringResource(R.string.login)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { 
                            tempUsername = it
                            loginError = null
                        },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { 
                            tempPassword = it
                            loginError = null
                        },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Show error message if login failed
                    loginError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Show loading indicator
                    if (isLoggingIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.logging_in),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempUsername.isBlank() || tempPassword.isBlank()) {
                            loginError = "Please enter both username and password"
                            return@TextButton
                        }
                        
                        isLoggingIn = true
                        loginError = null
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                LastFM.getMobileSession(tempUsername, tempPassword)
                                    .onSuccess { auth ->
                                        lastfmUsername = auth.session.name
                                        lastfmSession = auth.session.key
                                        LastFM.sessionKey = auth.session.key
                                        
                                        // Switch back to main thread to update UI
                                        coroutineScope.launch(Dispatchers.Main) {
                                            isLoggingIn = false
                                            showLoginDialog = false
                                            loginError = null
                                        }
                                    }
                                    .onFailure { exception ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            isLoggingIn = false
                                            loginError = when (exception) {
                                                is com.metrolist.lastfm.LastFM.LastFmException -> {
                                                    when (exception.code) {
                                                        4 -> "Invalid username or password"
                                                        6 -> "Invalid parameters"
                                                        9 -> "Invalid session key"
                                                        10 -> "Invalid API key"
                                                        13 -> "Invalid method signature"
                                                        14 -> "Unauthorized token"
                                                        15 -> "Service temporarily unavailable"
                                                        else -> "Login failed: ${exception.message}"
                                                    }
                                                }
                                                else -> "Network error. Please check your connection."
                                            }
                                        }
                                        reportException(exception)
                                    }
                            } catch (e: Exception) {
                                coroutineScope.launch(Dispatchers.Main) {
                                    isLoggingIn = false
                                    loginError = "Unexpected error occurred"
                                }
                                reportException(e)
                            }
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    Text(stringResource(R.string.login))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isLoggingIn) {
                            showLoginDialog = false
                            loginError = null
                        }
                    },
                    enabled = !isLoggingIn
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

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

        PreferenceGroupTitle(
            title = stringResource(R.string.account),
        )

        PreferenceEntry(
            title = {
                Text(
                    text = if (isLoggedIn) lastfmUsername else stringResource(R.string.not_logged_in),
                    modifier = Modifier.alpha(if (isLoggedIn) 1f else 0.5f),
                )
            },
            description = null,
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            trailingContent = {
                if (isLoggedIn) {
                    OutlinedButton(onClick = {
                        lastfmSession = ""
                        lastfmUsername = ""
                    }) {
                        Text(stringResource(R.string.action_logout))
                    }
                } else {
                    OutlinedButton(onClick = {
                        showLoginDialog = true
                    }) {
                        Text(stringResource(R.string.action_login))
                    }
                }
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.options),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_scrobbling)) },
            checked = lastfmScrobbling,
            onCheckedChange = onlastfmScrobblingChange,
            isEnabled = isLoggedIn,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lastfm_now_playing)) },
            checked = useNowPlaying,
            onCheckedChange = onUseNowPlayingChange,
            isEnabled = isLoggedIn && lastfmScrobbling,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.last_fm_send_likes)) },
            description = stringResource(R.string.last_fm_send_likes_description),
            checked = useSendLikes,
            onCheckedChange = onUseSendLikes,
            isEnabled = isLoggedIn,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.scrobbling_configuration)
        )

        var showMinTrackDurationDialog by rememberSaveable { mutableStateOf(false) }

        if (showMinTrackDurationDialog) {
            var tempMinTrackDuration by remember { mutableIntStateOf(minTrackDuration) }

            DefaultDialog(
                onDismiss = {
                    tempMinTrackDuration = minTrackDuration
                    showMinTrackDurationDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempMinTrackDuration = LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempMinTrackDuration = minTrackDuration
                            showMinTrackDurationDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onMinTrackDurationChange(tempMinTrackDuration)
                            showMinTrackDurationDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scrobble_min_track_duration),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = makeTimeString((tempMinTrackDuration * 1000).toLong()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempMinTrackDuration.toFloat(),
                        onValueChange = { tempMinTrackDuration = it.toInt() },
                        valueRange = 10f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.scrobble_min_track_duration)) },
            description = makeTimeString((minTrackDuration * 1000).toLong()),
            onClick = { showMinTrackDurationDialog = true }
        )

        var showScrobbleDelayPercentDialog by rememberSaveable { mutableStateOf(false) }

        if (showScrobbleDelayPercentDialog) {
            var tempScrobbleDelayPercent by remember { mutableFloatStateOf(scrobbleDelayPercent) }

            DefaultDialog(
                onDismiss = {
                    tempScrobbleDelayPercent = scrobbleDelayPercent
                    showScrobbleDelayPercentDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempScrobbleDelayPercent = LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempScrobbleDelayPercent = scrobbleDelayPercent
                            showScrobbleDelayPercentDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onScrobbleDelayPercentChange(tempScrobbleDelayPercent)
                            showScrobbleDelayPercentDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scrobble_delay_percent),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(R.string.sensitivity_percentage, (tempScrobbleDelayPercent * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempScrobbleDelayPercent,
                        onValueChange = { tempScrobbleDelayPercent = it },
                        valueRange = 0.3f..0.95f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.scrobble_delay_percent)) },
            description = stringResource(R.string.sensitivity_percentage, (scrobbleDelayPercent * 100).roundToInt()),
            onClick = { showScrobbleDelayPercentDialog = true }
        )

        var showScrobbleDelaySecondsDialog by rememberSaveable { mutableStateOf(false) }

        if (showScrobbleDelaySecondsDialog) {
            var tempScrobbleDelaySeconds by remember { mutableIntStateOf(scrobbleDelaySeconds) }

            DefaultDialog(
                onDismiss = {
                    tempScrobbleDelaySeconds = scrobbleDelaySeconds
                    showScrobbleDelaySecondsDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempScrobbleDelaySeconds = LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempScrobbleDelaySeconds = scrobbleDelaySeconds
                            showScrobbleDelaySecondsDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onScrobbleDelaySecondsChange(tempScrobbleDelaySeconds)
                            showScrobbleDelaySecondsDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scrobble_delay_minutes),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = makeTimeString((tempScrobbleDelaySeconds * 1000).toLong()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempScrobbleDelaySeconds.toFloat(),
                        onValueChange = { tempScrobbleDelaySeconds = it.toInt() },
                        valueRange = 30f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.scrobble_delay_minutes)) },
            description = makeTimeString((scrobbleDelaySeconds * 1000).toLong()),
            onClick = { showScrobbleDelaySecondsDialog = true }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lastfm_integration)) },
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
