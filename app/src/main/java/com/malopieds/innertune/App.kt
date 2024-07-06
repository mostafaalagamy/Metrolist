package com.malopieds.innertune

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
import com.malopieds.innertune.constants.ContentCountryKey
import com.malopieds.innertune.constants.ContentLanguageKey
import com.malopieds.innertune.constants.CountryCodeToName
import com.malopieds.innertune.constants.InnerTubeCookieKey
import com.malopieds.innertune.constants.LanguageCodeToName
import com.malopieds.innertune.constants.MaxImageCacheSizeKey
import com.malopieds.innertune.constants.ProxyEnabledKey
import com.malopieds.innertune.constants.ProxyTypeKey
import com.malopieds.innertune.constants.ProxyUrlKey
import com.malopieds.innertune.constants.SYSTEM_DEFAULT
import com.malopieds.innertune.constants.VisitorDataKey
import com.malopieds.innertune.extensions.toEnum
import com.malopieds.innertune.extensions.toInetSocketAddress
import com.malopieds.innertune.utils.dataStore
import com.malopieds.innertune.utils.get
import com.malopieds.innertune.utils.reportException
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
