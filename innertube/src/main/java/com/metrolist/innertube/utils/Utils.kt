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

fun nSigDecode(n: String): String {
    val step1 =
        buildString {
            append(n[8])
            append(n.substring(2, 8))
            append(n[1])
            append(n.substring(9))
        }
    val step2 =
        buildString {
            append(step1.substring(7))
            append((step1[0] + step1.substring(1, 3).reversed() + step1[3]).reversed())
            append(step1.substring(4, 7))
        }
    val step3 = step2.substring(7) + step2.substring(0, 7)
    val step4 =
        buildString {
            append(step3[step3.length - 4])
            append(step3.substring(3, 7))
            append(step3[2])
            append(step3.substring(8, 11))
            append(step3[7])
            append(step3.takeLast(3))
            append(step3[1])
        }
    val step5 = (step4.substring(0, 2) + step4.last() + step4.substring(3, step4.length - 1) + step4[2]).reversed()
    val keyString = "cbrrC5"
    val charset = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '_')
    val mutableKeyList = keyString.toMutableList()
    val transformedChars = CharArray(step5.length)
    for (index in step5.indices) {
        val currentChar = step5[index]
        val indexInCharset =
            (charset.indexOf(currentChar) - charset.indexOf(mutableKeyList[index % mutableKeyList.size]) + index + charset.size - index) %
                charset.size
        transformedChars[index] = charset[indexInCharset]
        mutableKeyList[index % mutableKeyList.size] = transformedChars[index]
    }
    val step6 = String(transformedChars)
    return step6.dropLast(3).reversed() + step6.takeLast(3)
}
fun sigDecode(input: String): String {
    val middleSection = input.substring(3, input.length - 3)
    val rearranged = (middleSection.take(35) + input[0] + middleSection.drop(36)).reversed()
    val result =
        buildString {
            append("A")
            append(rearranged.substring(0, 15))
            append(input[input.length - 2])
            append(rearranged.substring(16, 34))
            append(input[input.length - 3])
            append(rearranged.substring(35))
            append(input[38])
        }
    return result
}
