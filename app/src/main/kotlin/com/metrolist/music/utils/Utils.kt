package com.metrolist.music.utils

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import java.util.Locale

const val MAX_SYNC_JOBS = 3

val syncCoroutine = Dispatchers.IO.limitedParallelism(MAX_SYNC_JOBS)

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

@Suppress("DEPRECATION")
fun setAppLocale(context: Context, locale: Locale) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}