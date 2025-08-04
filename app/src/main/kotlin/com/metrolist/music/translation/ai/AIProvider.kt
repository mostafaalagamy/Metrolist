package com.metrolist.music.translation.ai

/**
 * Interface for AI translation providers
 */
interface AIProvider {
    suspend fun translate(prompt: String, originalText: String): String
}