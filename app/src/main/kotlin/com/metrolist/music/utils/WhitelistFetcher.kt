package com.metrolist.music.utils

import com.metrolist.music.db.entities.ArtistWhitelistEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import java.time.LocalDateTime

object WhitelistFetcher {
    private val client = HttpClient()
    private const val WHITELIST_URL = "https://api.github.com/repos/alltechdev/ytmusicjson/contents/artists.json?ref=main"

    var lastFetchTime = -1L
        private set

    suspend fun fetchWhitelist(): Result<List<ArtistWhitelistEntity>> =
        runCatching {
            val response = client.get(WHITELIST_URL).bodyAsText()
            val json = JSONObject(response)

            // GitHub API returns base64 encoded content
            val content = json.getString("content")
            val decodedContent = String(android.util.Base64.decode(content, android.util.Base64.DEFAULT))
            val artistsJson = JSONObject(decodedContent)
            val artistsArray = artistsJson.getJSONArray("artists")

            val now = LocalDateTime.now()
            val whitelistEntities = mutableListOf<ArtistWhitelistEntity>()

            for (i in 0 until artistsArray.length()) {
                val artistObj = artistsArray.getJSONObject(i)
                val artistId = artistObj.getString("id")
                val artistName = artistObj.getString("name")

                whitelistEntities.add(
                    ArtistWhitelistEntity(
                        artistId = artistId,
                        artistName = artistName,
                        addedAt = now,
                        source = "github",
                        lastSyncedAt = now
                    )
                )
            }

            lastFetchTime = System.currentTimeMillis()
            whitelistEntities
        }
}
