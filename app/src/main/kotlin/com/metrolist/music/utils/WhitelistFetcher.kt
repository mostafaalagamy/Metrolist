package com.metrolist.music.utils

import com.metrolist.music.db.entities.ArtistWhitelistEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDateTime

object WhitelistFetcher {
    private val client = HttpClient()
    // Use raw.githubusercontent.com for direct JSON access (faster, no base64 decoding needed)
    private const val WHITELIST_URL = "https://raw.githubusercontent.com/alltechdev/ytmusicjson/main/artists.json"

    var lastFetchTime = -1L
        private set

    suspend fun fetchWhitelist(): Result<List<ArtistWhitelistEntity>> =
        runCatching {
            val response = client.get(WHITELIST_URL).bodyAsText()

            // Direct JSON parsing (no base64 decoding needed with raw URL)
            val artistsJson = JSONObject(response)
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

    /**
     * Parses a JSON string into whitelist entities.
     * Used after fetching to avoid re-downloading.
     */
    fun parseWhitelistJson(jsonString: String): List<ArtistWhitelistEntity> {
        val artistsJson = JSONObject(jsonString)
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

        return whitelistEntities
    }

    /**
     * Fetches the whitelist JSON and computes its SHA-256 hash.
     * This is used to detect changes without parsing the entire JSON.
     * @return Pair of (JSON string, hash string) or failure
     */
    suspend fun fetchWhitelistHash(): Result<Pair<String, String>> =
        runCatching {
            val jsonString = client.get(WHITELIST_URL).bodyAsText()
            val hash = computeHash(jsonString)
            Pair(jsonString, hash)
        }

    /**
     * Computes SHA-256 hash of a string
     */
    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
