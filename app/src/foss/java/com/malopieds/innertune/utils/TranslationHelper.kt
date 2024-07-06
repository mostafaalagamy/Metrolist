package com.malopieds.innertune.utils

import com.malopieds.innertune.db.entities.LyricsEntity

object TranslationHelper {
    suspend fun translate(lyrics: LyricsEntity): LyricsEntity = lyrics

    suspend fun clearModels() {}
}
