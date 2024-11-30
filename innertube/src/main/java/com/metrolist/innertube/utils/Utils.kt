package com.metrolist.innertube.utils

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.PlaylistPage
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
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

fun String.swap(index1: Int, index2: Int): String {
    val chars = toCharArray()
    val temp = chars[index1]
    chars[index1] = chars[index2]
    chars[index2] = temp
    return String(chars)
}

fun String.rotateLeft(n: Int): String = substring(n) + substring(0, n)

fun String.rotateRight(n: Int): String = takeLast(n) + dropLast(n)

fun String.removeIndex(index: Int): String = removeRange(index, index + 1)

fun transform(input: String, key: String, charset: List<Char>): String {
    val keyList = key.toMutableList()
    val keyLength = key.length
    return buildString {
        input.forEachIndexed { idx, char ->
            val transformedChar = charset[
                (charset.indexOf(char) - charset.indexOf(keyList[idx % keyLength]) + idx + charset.size - idx) % charset.size
            ]
            append(transformedChar)
            keyList[idx % keyLength] = transformedChar
        }
    }
}

fun String.sliceSegment(start: Int, end: Int): String = substring(start, end)

fun String.sliceFrom(start: Int): String = substring(start)


fun nSigDecode(n: String): String {
    val step1 = n.swap(0, 3)
    val step2 = step1.swap(0, 14)
    val step3 = step2.reversed()
    val step4 = step3.swap(0, 17)
    val step5 = step4.rotateRight(3)
    val step6 = step5.reversed()
    val step7 = step6.swap(0, 12)
    val step8 = step7.reversed()
    val step9 = step8.removeIndex(0).removeIndex(0)

    val cipherKey = "pdENIJ6"
    val charset = listOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
        'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-', '_'
    )

    val transformed = transform(step9, cipherKey, charset)

    val result = transformed
        .rotateLeft(10)
        .rotateLeft(8)
        .rotateLeft(8)
        .removeIndex(8)
        .reversed()
        .rotateLeft(8)
        .removeIndex(6)
    return result.dropLast(2) + result.last()
}

fun sigDecode(input: String): String {
    val result = input.sliceSegment(6, 11) +
            input[65] +
            input.sliceSegment(12, 65) +
            input[0] +
            input.sliceFrom(66)
    return result.removeIndex(result.length - 1)
}

fun createUrl(
    url: String? = null,
    cipher: String? = null,
): String? {
    val resUrl: URLBuilder
    var signature = ""
    var signatureParam = "sig"
    if (cipher != null) {
        val params = parseQueryString(cipher)
        signature = params["s"] ?: return null
        signatureParam = params["sp"] ?: return null
        resUrl = params["url"]?.let { URLBuilder(it) } ?: return null
    } else {
        resUrl = url?.let { URLBuilder(it) } ?: return null
    }
    val n = resUrl.parameters["n"]
    resUrl.parameters["n"] = nSigDecode(n.toString())
    if (cipher != null) {
        resUrl.parameters[signatureParam] = sigDecode(signature)
    }
    resUrl.parameters["c"] = "ANDROID_MUSIC"
    println(signature)
    println(n)
    println(resUrl)
    return resUrl.toString()
}
