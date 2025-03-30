package com.metrolist.music.ui.screens.settings

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.toLowerCase
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.*
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.LocaleManager
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import java.net.Proxy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager(context) }
    val languages = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (selectedLanguage, setSelectedLanguage) = rememberPreference(stringPreferencesKey("app_language"), "en")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (preferredProvider, onPreferredProviderChange) =
        rememberEnumPreference(
            key = PreferredLyricsProviderKey,
            defaultValue = PreferredLyricsProvider.LRCLIB,
        )
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.general))
        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = { newValue ->
                val locale = Locale.getDefault()
                val languageTag = locale.toLanguageTag().replace("-Hant", "")
 
                YouTube.locale = YouTube.locale.copy(
                    hl = newValue.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.language.takeIf { it in LanguageCodeToName }
                        ?: languageTag.takeIf { it in LanguageCodeToName }
                        ?: "en"
                )
 
                onContentLanguageChange(newValue)
            }
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = { newValue ->
                val locale = Locale.getDefault()
 
                YouTube.locale = YouTube.locale.copy(
                    gl = newValue.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.country.takeIf { it in CountryCodeToName }
                        ?: "US"
                )
 
                onContentCountryChange(newValue)
           }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_explicit)) },
            icon = { Icon(painterResource(R.drawable.explicit), null) },
            checked = hideExplicit,
            onCheckedChange = onHideExplicitChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.app_language))
        ListPreference(
            title = { Text(stringResource(R.string.app_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = selectedLanguage,
            values = languages,
            valueText = { code ->
                LanguageCodeToName[code] ?: stringResource(R.string.system_default)
            },
            onValueSelected = { newLanguage ->
                if (localeManager.updateLocale(newLanguage)) {
                    setSelectedLanguage(newLanguage)

                    val intent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(
                        context,
                        "Failed to update language. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        PreferenceGroupTitle(title = stringResource(R.string.proxy))
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange,
        )
        if (proxyEnabled) {
            Column {
                ListPreference(
                    title = { Text(stringResource(R.string.proxy_type)) },
                    selectedValue = proxyType,
                    values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                    valueText = { it.name },
                    onValueSelected = onProxyTypeChange,
                )
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_url)) },
                    value = proxyUrl,
                    onValueChange = onProxyUrlChange,
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.lyrics))
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableLrclib,
            onCheckedChange = onEnableLrclibChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange,
        )
        ListPreference(
            title = { Text(stringResource(R.string.set_first_lyrics_provider)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = preferredProvider,
            values = listOf(PreferredLyricsProvider.KUGOU, PreferredLyricsProvider.LRCLIB),
            valueText = {
                it.name.toLowerCase(androidx.compose.ui.text.intl.Locale.current)
                    .capitalize(androidx.compose.ui.text.intl.Locale.current)
            },
            onValueSelected = onPreferredProviderChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.misc))
        EditTextPreference(
            title = { Text(stringResource(R.string.top_length)) },
            icon = { Icon(painterResource(R.drawable.trending_up), null) },
            value = lengthTop,
            isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
            onValueChange = onLengthTopChange,
        )
        ListPreference(
            title = { Text(stringResource(R.string.set_quick_picks)) },
            icon = { Icon(painterResource(R.drawable.home_outlined), null) },
            selectedValue = quickPicks,
            values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
            valueText = {
                when (it) {
                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                }
            },
            onValueSelected = onQuickPicksChange,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.content)) },
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
