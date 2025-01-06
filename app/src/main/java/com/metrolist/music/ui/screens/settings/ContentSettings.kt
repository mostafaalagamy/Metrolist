package com.metrolist.music.ui.screens.settings

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.datastore.preferences.core.*
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.*
import com.metrolist.music.ui.component.*
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import java.net.Proxy
import java.util.Locale

object PreferenceKeys {
    val ContentLanguageKey = stringPreferencesKey("content_language")
    val ContentCountryKey = stringPreferencesKey("content_country")
    val HideExplicitKey = booleanPreferencesKey("hide_explicit")
    val ProxyEnabledKey = booleanPreferencesKey("proxy_enabled")
    val ProxyTypeKey = stringPreferencesKey("proxy_type")
    val ProxyUrlKey = stringPreferencesKey("proxy_url")
    val TopSizeKey = stringPreferencesKey("top_size")
    val HistoryDurationKey = floatPreferencesKey("history_duration")
    val QuickPicksKey = stringPreferencesKey("quick_picks")
    val EnableKugouKey = booleanPreferencesKey("enable_kugou")
    val EnableLrcLibKey = booleanPreferencesKey("enable_lrclib")
    val PreferredLyricsProviderKey = stringPreferencesKey("preferred_lyrics_provider")
    val AppLanguageKey = stringPreferencesKey("app_language")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager(context) }
    
    val (contentLanguage, onContentLanguageChange) = rememberPreference(
        key = PreferenceKeys.ContentLanguageKey,
        defaultValue = "system"
    )
    val (contentCountry, onContentCountryChange) = rememberPreference(
        key = PreferenceKeys.ContentCountryKey,
        defaultValue = "system"
    )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(
        key = PreferenceKeys.HideExplicitKey,
        defaultValue = false
    )
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(
        key = PreferenceKeys.ProxyEnabledKey,
        defaultValue = false
    )
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(
        key = PreferenceKeys.ProxyTypeKey,
        defaultValue = Proxy.Type.HTTP
    )
    val (proxyUrl, onProxyUrlChange) = rememberPreference(
        key = PreferenceKeys.ProxyUrlKey,
        defaultValue = "host:port"
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(
        key = PreferenceKeys.TopSizeKey,
        defaultValue = "50"
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        key = PreferenceKeys.HistoryDurationKey,
        defaultValue = 30f
    )
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(
        key = PreferenceKeys.QuickPicksKey,
        defaultValue = QuickPicks.QUICK_PICKS
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(
        key = PreferenceKeys.EnableKugouKey,
        defaultValue = true
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = PreferenceKeys.EnableLrcLibKey,
        defaultValue = true
    )
    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(
        key = PreferenceKeys.PreferredLyricsProviderKey,
        defaultValue = PreferredLyricsProvider.LRCLIB
    )
    val (selectedLanguage, setSelectedLanguage) = rememberPreference(
        key = PreferenceKeys.AppLanguageKey,
        defaultValue = "en"
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        // General settings
        PreferenceGroupTitle(title = stringResource(R.string.general))
        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = onContentLanguageChange,
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = onContentCountryChange,
        )

        // Hide explicit content
        SwitchPreference(
            title = { Text(stringResource(R.string.hide_explicit)) },
            icon = { Icon(painterResource(R.drawable.explicit), null) },
            checked = hideExplicit,
            onCheckedChange = onHideExplicitChange,
        )

        // Language settings
        PreferenceGroupTitle(title = stringResource(R.string.app_language))
        ListPreference(
            title = { Text(stringResource(R.string.app_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = selectedLanguage,
            values = LanguageCodeToName.keys.toList(),
            valueText = { LanguageCodeToName[it] ?: stringResource(R.string.system_default) },
            onValueSelected = { newLanguage ->
                if (localeManager.updateLocale(newLanguage)) {
                    setSelectedLanguage(newLanguage)
                    
                    // Restart activity to apply changes
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

        // Proxy settings
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

        // Lyrics settings
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
                it.name.toLowerCase(androidx.compose.ui.text.intl.Locale.current).capitalize(androidx.compose.ui.text.intl.Locale.current)
            },
            onValueSelected = onPreferredProviderChange,
        )

        // Misc settings
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
        SliderPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            value = historyDuration,
            onValueChange = onHistoryDurationChange,
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
        },
    )
}

// LocaleManager
class LocaleManager(private val context: Context) {
    companion object {
        private val COMPLEX_SCRIPT_LANGUAGES = setOf(
            "ne", "mr", "hi", "bn", "pa", "gu", "ta", "te", "kn", "ml", 
            "si", "th", "lo", "my", "ka", "am", "km",
            "zh-CN", "zh-TW", "zh-HK", "ja", "ko"
        )
    }

    fun updateLocale(languageCode: String): Boolean {
        try {
            val locale = createLocaleFromCode(languageCode)
            val config = context.resources.configuration

            Locale.setDefault(locale)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocaleApi24(config, locale)
            } else {
                setLocaleLegacy(config, locale)
            }

            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            val newContext = context.createConfigurationContext(config)
            updateAppContext(newContext)
            
            return true
        } catch (e: Exception) {
            Log.e("LocaleManager", "Failed to update locale", e)
            return false
        }
    }

    private fun createLocaleFromCode(languageCode: String): Locale {
        return when {
            languageCode == "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            languageCode == "zh-TW" -> Locale.TRADITIONAL_CHINESE
            languageCode == "zh-HK" -> Locale("zh", "HK")
            
            languageCode in COMPLEX_SCRIPT_LANGUAGES -> {
                if (languageCode.contains("-")) {
                    val (language, country) = languageCode.split("-")
                    Locale.Builder()
                        .setLanguage(language)
                        .setRegion(country)
                        .setScript(getScriptForLanguage(languageCode))
                        .build()
                } else {
                    Locale.Builder()
                        .setLanguage(languageCode)
                        .setScript(getScriptForLanguage(languageCode))
                        .build()
                }
            }
            
            languageCode.contains("-") -> {
                val (language, country) = languageCode.split("-")
                Locale(language, country)
            }
            
            else -> Locale(languageCode)
        }
    }

    private fun getScriptForLanguage(languageCode: String): String {
        return when (languageCode) {
            "hi", "mr" -> "Deva" // Devanagari
            "bn" -> "Beng" // Bengali
            "pa" -> "Guru" // Gurmukhi
            "gu" -> "Gujr" // Gujarati
            "ta" -> "Taml" // Tamil
            "te" -> "Telu" // Telugu
            "kn" -> "Knda" // Kannada
            "ml" -> "Mlym" // Malayalam
            "si" -> "Sinh" // Sinhala
            "th" -> "Thai" // Thai
            "ka" -> "Geor" // Georgian
            "am" -> "Ethi" // Ethiopic
            "km" -> "Khmr" // Khmer
            else -> ""
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setLocaleApi24(config: Configuration, locale: Locale) {
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        config.setLocales(localeList)
    }

    @Suppress("DEPRECATION")
    private fun setLocaleLegacy(config: Configuration, locale: Locale) {
        config.locale = locale
    }

    private fun updateAppContext(newContext: Context) {
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val thread = activityThread.getMethod("currentActivityThread").invoke(null)
            val application = activityThread.getMethod("getApplication").invoke(thread)
            val appContext = application.javaClass.getMethod("getBaseContext").invoke(application)
            
            val contextImpl = Class.forName("android.app.ContextImpl")
            val implResources = contextImpl.getDeclaredField("mResources")
            implResources.isAccessible = true
            implResources.set(appContext, newContext.resources)
        } catch (e: Exception) {
            Log.e("LocaleManager", "Failed to update app context", e)
        }
    }
}

// Language mappings
val LanguageCodeToName = mapOf(
    "ar" to "العربية",
    "en" to "English",
    "fr" to "Français",
    "es" to "Español (España)",
    "it" to "Italiano",
    "de" to "Deutsch",
    "nl" to "Nederlands",
    "pt-PT" to "Português",
    "pt" to "Português (Brasil)",
    "ru" to "Русский",
    "tr" to "Türkçe",
    "id" to "Bahasa Indonesia",
    "ur" to "اردو",
    "fa" to "فارسی",
    "ne" to "नेपाली",
    "mr" to "मराठी",
    "hi" to "हिन्दी",
    "bn" to "বাংলা",
    "pa" to "ਪੰਜਾਬੀ",
    "gu" to "ગુજરાતી",
    "ta" to "தமிழ்",
    "te" to "తెలుగు",
    "kn" to "ಕನ್ನಡ",
    "ml" to "മലയാളം",
    "si" to "සිංහල",
    "th" to "ภาษาไทย",
    "lo" to "ລາວ",
    "my" to "ဗမာ",
    "ka" to "ქართული",
    "am" to "አማርኛ",
    "km" to "ខ្មែរ",
    "zh-CN" to "中文 (简体)",
    "zh-TW" to "中文 (繁體)",
    "zh-HK" to "中文 (香港)",
    "ja" to "日本語",
    "ko" to "한국어",
)
