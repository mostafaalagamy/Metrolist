package com.metrolist.innertube.utils

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.PlaylistPage
import java.security.MessageDigest

suspend fun Result<PlaylistPage>.completed() =
    runCatching {
        val page = getOrThrow()
        val songs = page.songs.toMutableList()
        var continuation = page.songsContinuation
        while (continuation != null) {
            val continuationPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
            songs += continuationPage.songs
            continuation = continuationPage.continuation
        }
        PlaylistPage(
            playlist = page.playlist,
            songs = songs,
            songsContinuation = null,
            continuation = page.continuation,
        )
    }

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

fun parseCookieString(cookie: String): Map<String, String> =
    cookie
        .split("; ")
        .filter { it.isNotEmpty() }
        .associate {
            val (key, value) = it.split("=")
            key to value
        }

fun String.parseTime(): Int? {
    try {
        val parts = split(":").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}
