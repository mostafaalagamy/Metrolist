package com.metrolist.music.translation.traditional

/**
 * Interface for traditional translation providers
 */
interface TraditionalProvider {
    suspend fun translate(text: String, sourceLangCode: String, targetLangCode: String): String
}