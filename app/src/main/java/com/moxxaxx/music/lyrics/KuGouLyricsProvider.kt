package com.moxxaxx.music.lyrics

import android.content.Context
import com.moxxaxx.music.constants.EnableKugouKey
import com.moxxaxx.music.utils.dataStore
import com.moxxaxx.music.utils.get
import com.moxxaxx.kugou.KuGou

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        KuGou.getAllLyrics(title, artist, duration, callback)
    }
}
