package com.moxxaxx.music

import android.app.Application
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.YouTubeLocale
import com.moxxaxx.music.constants.ContentCountryKey
import com.moxxaxx.music.constants.ContentLanguageKey
import com.moxxaxx.music.constants.CountryCodeToName
import com.moxxaxx.music.constants.InnerTubeCookieKey
import com.moxxaxx.music.constants.LanguageCodeToName
import com.moxxaxx.music.constants.MaxImageCacheSizeKey
import com.moxxaxx.music.constants.ProxyEnabledKey
import com.moxxaxx.music.constants.ProxyTypeKey
import com.moxxaxx.music.constants.ProxyUrlKey
import com.moxxaxx.music.constants.SYSTEM_DEFAULT
import com.moxxaxx.music.constants.VisitorDataKey
import com.moxxaxx.music.extensions.toEnum
import com.moxxaxx.music.extensions.toInetSocketAddress
import com.moxxaxx.music.utils.dataStore
import com.moxxaxx.music.utils.get
import com.moxxaxx.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App :
    Application(),
    ImageLoaderFactory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale =
            YouTubeLocale(
                gl =
                    dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.country.takeIf { it in CountryCodeToName }
                        ?: "US",
                hl =
                    dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.language.takeIf { it in LanguageCodeToName }
                        ?: languageTag.takeIf { it in LanguageCodeToName }
                        ?: "en",
            )

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy =
                    Proxy(
                        dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                        dataStore[ProxyUrlKey]!!.toInetSocketAddress(),
                    )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        } ?: YouTube.DEFAULT_VISITOR_DATA
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    YouTube.cookie = cookie
                }
        }
    }

    override fun newImageLoader() =
        ImageLoader
            .Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((dataStore[MaxImageCacheSizeKey] ?: 512) * 1024 * 1024L)
                    .build(),
            ).build()
}
