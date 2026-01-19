/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/mostafaalagamy/Metrolist/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            val versionName = json.getString("name")
            lastCheckTime = System.currentTimeMillis()
            versionName
        }

    fun getLatestDownloadUrl(): String {
        val baseUrl = "https://github.com/mostafaalagamy/Metrolist/releases/latest/download/"
        val architecture = BuildConfig.ARCHITECTURE
        val isGmsVariant = BuildConfig.CAST_AVAILABLE
        
        return if (architecture == "universal") {
            if (isGmsVariant) {
                baseUrl + "Metrolist-with-Google-Cast.apk"
            } else {
                baseUrl + "Metrolist.apk"
            }
        } else {
            if (isGmsVariant) {
                baseUrl + "app-${architecture}-with-Google-Cast.apk"
            } else {
                baseUrl + "app-${architecture}-release.apk"
            }
        }
    }
}
