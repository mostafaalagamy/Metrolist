package com.moxxaxx.music.utils

import com.moxxaxx.music.db.entities.LyricsEntity

object TranslationHelper {
    suspend fun translate(lyrics: LyricsEntity): LyricsEntity = lyrics

    suspend fun clearModels() {}
}
