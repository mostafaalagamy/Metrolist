@file:Suppress("DEPRECATION")

package com.metrolist.music.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import java.util.Locale

class LocaleManager(private val context: Context) {
    companion object {
        private val COMPLEX_SCRIPT_LANGUAGES = setOf(
            "ar", "he", "ne", "mr", "hi", "bn", "pa", "gu", "ta", "te",
            "kn", "ml", "si", "th", "lo", "my", "ka", "am", "km",
            "zh-CN", "zh-TW", "zh-HK", "ja", "ko"
        )

        fun applySavedLocale(context: Context): Context {
            val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val languageCode = prefs.getString("app_language", "en") ?: "en"
            return LocaleManager(context).wrapContext(context, languageCode)
        }
    }

    fun updateLocale(languageCode: String): Boolean {
        try {
            val locale = createLocaleFromCode(languageCode)
            val resources = context.resources
            val config = Configuration(resources.configuration)

            Locale.setDefault(locale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocaleApi24(config, locale)
            } else {
                setLocaleLegacy(config, locale)
            }

            resources.updateConfiguration(config, resources.displayMetrics)

            // Save to SharedPreferences
            val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            prefs.edit().putString("app_language", languageCode).apply()

            return true
        } catch (e: Exception) {
            Log.e("LocaleManager", "Failed to update locale", e)
            return false
        }
    }

    private fun wrapContext(context: Context, languageCode: String): Context {
        val locale = createLocaleFromCode(languageCode)
        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setLocaleApi24(config, locale)
        } else {
            setLocaleLegacy(config, locale)
        }

        return context.createConfigurationContext(config)
    }

    private fun createLocaleFromCode(languageCode: String): Locale {
        return when {
            languageCode == "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            languageCode == "zh-TW" -> Locale.TRADITIONAL_CHINESE
            languageCode == "zh-HK" -> Locale("zh", "HK")
            languageCode in COMPLEX_SCRIPT_LANGUAGES -> {
                if (languageCode.contains("-")) {
                    val (lang, country) = languageCode.split("-")
                    Locale.Builder().setLanguage(lang).setRegion(country).build()
                } else {
                    Locale.Builder().setLanguage(languageCode).build()
                }
            }
            else -> Locale(languageCode)
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
}
