package com.metrolist.music.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
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

    fun updateLocale(languageCode: String): Context {
        val locale = createLocaleFromCode(languageCode)
        val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", languageCode).apply()

        return wrapContext(context, languageCode)
    }

    private fun wrapContext(context: Context, languageCode: String): Context {
        val locale = createLocaleFromCode(languageCode)
        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            config.setLocale(locale)
            Locale.setDefault(locale)
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
}
