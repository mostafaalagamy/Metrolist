package com.metrolist.music.utils

import com.metrolist.music.db.entities.LyricsEntity

object TranslationHelper {
    suspend fun translate(lyrics: LyricsEntity): LyricsEntity = lyrics

    suspend fun clearModels() {}
}
