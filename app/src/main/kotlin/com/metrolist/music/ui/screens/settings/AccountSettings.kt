package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.App.Companion.forgetAccount
import com.metrolist.music.R
import com.metrolist.music.constants.*
import com.metrolist.music.ui.component.*
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val viewModel: HomeViewModel = hiltViewModel()
    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()

    var showToken: Boolean by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.close), contentDescription = "Close")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {

                PreferenceGroupTitle(title = stringResource(R.string.google))

                PreferenceEntry(
                    title = {
                        Text(if (isLoggedIn) accountName else stringResource(R.string.login))
                    },
                    description = if (isLoggedIn) {
                        accountEmail.ifEmpty { accountChannelHandle }
                    } else null,
                    icon = {
                        if (isLoggedIn && accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(painterResource(R.drawable.login), contentDescription = null)
                        }
                    },
                    trailingContent = {
                        if (isLoggedIn) {
                            OutlinedButton(onClick = {
                                onInnerTubeCookieChange("")
                                forgetAccount(context)
                            }) {
                                Text(stringResource(R.string.action_logout))
                            }
                        }
                    },
                    onClick = { if (!isLoggedIn) navController.navigate("login") }
                )

                if (showTokenEditor) {
                    val text = """
                        ***INNERTUBE COOKIE*** =${innerTubeCookie}
                        ***VISITOR DATA*** =${visitorData}
                        ***DATASYNC ID*** =${dataSyncId}
                        ***ACCOUNT NAME*** =${accountNamePref}
                        ***ACCOUNT EMAIL*** =${accountEmail}
                        ***ACCOUNT CHANNEL HANDLE*** =${accountChannelHandle}
                    """.trimIndent()
                    TextFieldDialog(
                        modifier = Modifier,
                        initialTextFieldValue = TextFieldValue(text),
                        onDone = { data ->
                            data.split("\n").forEach {
                                when {
                                    it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                                    it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                                    it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                                    it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                                    it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                                    it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                                }
                            }
                        },
                        onDismiss = { showTokenEditor = false },
                        singleLine = false,
                        maxLines = 20,
                        isInputValid = {
                            it.isNotEmpty() && try {
                                "SAPISID" in parseCookieString(it)
                            } catch (e: Exception) {
                                false
                            }
                        },
                        extraContent = {
                            InfoLabel(text = stringResource(R.string.token_adv_login_description))
                        }
                    )
                }

                PreferenceEntry(
                    title = {
                        Text(
                            when {
                                !isLoggedIn -> stringResource(R.string.advanced_login)
                                showToken -> stringResource(R.string.token_shown)
                                else -> stringResource(R.string.token_hidden)
                            }
                        )
                    },
                    icon = { Icon(painterResource(R.drawable.token), null) },
                    onClick = {
                        if (!isLoggedIn) showTokenEditor = true
                        else if (!showToken) showToken = true
                        else showTokenEditor = true
                    }
                )

                if (isLoggedIn) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.use_login_for_browse)) },
                        description = stringResource(R.string.use_login_for_browse_desc),
                        icon = { Icon(painterResource(R.drawable.person), null) },
                        checked = useLoginForBrowse,
                        onCheckedChange = {
                            YouTube.useLoginForBrowse = it
                            onUseLoginForBrowseChange(it)
                        }
                    )

                    SwitchPreference(
                        title = { Text(stringResource(R.string.ytm_sync)) },
                        icon = { Icon(painterResource(R.drawable.cached), null) },
                        checked = ytmSync,
                        onCheckedChange = onYtmSyncChange
                    )
                }
            }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(top = 8.dp)
            ) {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.discord_integration)) },
                    icon = { Icon(painterResource(R.drawable.discord), null) },
                    onClick = { navController.navigate("settings/discord") }
                )
            }
        }
    }
}
