package com.zionhuang.music.ui.screens.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zionhuang.innertube.utils.parseCookieString
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.R
import com.zionhuang.music.constants.AccountChannelHandleKey
import com.zionhuang.music.constants.AccountEmailKey
import com.zionhuang.music.constants.AccountNameKey
import com.zionhuang.music.constants.ChipSortTypeKey
import com.zionhuang.music.constants.ContentCountryKey
import com.zionhuang.music.constants.ContentLanguageKey
import com.zionhuang.music.constants.CountryCodeToName
import com.zionhuang.music.constants.InnerTubeCookieKey
import com.zionhuang.music.constants.LanguageCodeToName
import com.zionhuang.music.constants.LibraryFilter.*
import com.zionhuang.music.constants.ProxyEnabledKey
import com.zionhuang.music.constants.ProxyTypeKey
import com.zionhuang.music.constants.ProxyUrlKey
import com.zionhuang.music.constants.QuickPicks.*
import com.zionhuang.music.constants.QuickPicksKey
import com.zionhuang.music.constants.SYSTEM_DEFAULT
import com.zionhuang.music.constants.TopSize
import com.zionhuang.music.ui.component.EditTextPreference
import com.zionhuang.music.ui.component.IconButton
import com.zionhuang.music.ui.component.ListPreference
import com.zionhuang.music.ui.component.PreferenceEntry
import com.zionhuang.music.ui.component.PreferenceGroupTitle
import com.zionhuang.music.ui.component.SwitchPreference
import com.zionhuang.music.ui.utils.backToMain
import com.zionhuang.music.utils.rememberEnumPreference
import com.zionhuang.music.utils.rememberPreference
import java.net.Proxy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(key = ChipSortTypeKey, defaultValue = LIBRARY)
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QUICK_PICKS)


    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceEntry(
            title = { Text(if (isLoggedIn) accountName else stringResource(R.string.login)) },
            description = if (isLoggedIn) {
                accountEmail.takeIf { it.isNotEmpty() }
                    ?: accountChannelHandle.takeIf { it.isNotEmpty() }
            } else null,
            icon = { Icon(painterResource(R.drawable.person), null) },
            onClick = { navController.navigate("login") }
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentLanguageChange
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentCountryChange
        )

        PreferenceGroupTitle(
            title = "PROXY"
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange
        )

        if (proxyEnabled) {
            ListPreference(
                title = { Text(stringResource(R.string.proxy_type)) },
                selectedValue = proxyType,
                values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                valueText = { it.name },
                onValueSelected = onProxyTypeChange
            )
            EditTextPreference(
                title = { Text(stringResource(R.string.proxy_url)) },
                value = proxyUrl,
                onValueChange = onProxyUrlChange
            )
        }

        EditTextPreference(
            title = { Text(stringResource(R.string.top_length)) },
            value = lengthTop,
            isInputValid = {val number = it.toIntOrNull()
                number != null && it.isNotEmpty() &&number > 0
                           },
            onValueChange = onLengthTopChange
        )

        ListPreference(
            title = { Text(stringResource(R.string.default_lib_chips)) },
            selectedValue = defaultChip,
            values = listOf(LIBRARY, PLAYLISTS, SONGS, ALBUMS, ARTISTS),
            valueText = {
                when(it) {
                    SONGS -> stringResource(R.string.songs)
                    ARTISTS -> stringResource(R.string.artists)
                    ALBUMS -> stringResource(R.string.albums)
                    PLAYLISTS -> stringResource(R.string.playlists)
                    LIBRARY -> stringResource(R.string.filter_library)
                }
            },
            onValueSelected = onDefaultChipChange
        )

        ListPreference(
            title = { Text(stringResource(R.string.set_quick_picks)) },
            selectedValue = quickPicks,
            values = listOf(QUICK_PICKS, LAST_LISTEN),
            valueText = {
                when (it) {
                    QUICK_PICKS -> stringResource(R.string.quick_picks)
                    LAST_LISTEN -> stringResource(R.string.last_song_listened)
                }
            },
            onValueSelected = onQuickPicksChange
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.content)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
