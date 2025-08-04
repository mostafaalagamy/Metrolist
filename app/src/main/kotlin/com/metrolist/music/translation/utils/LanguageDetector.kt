package com.metrolist.music.translation.utils

/**
 * Language detection utility
 */
object LanguageDetector {
    
    /**
     * Detect the language of the given text
     */
    fun detectLanguage(text: String): String {
        if (text.isEmpty()) return "English"
        
        return when {
            isArabic(text) -> "Arabic"
            isJapanese(text) -> "Japanese"
            isKorean(text) -> "Korean"
            isChinese(text) -> "Chinese"
            isRussian(text) -> "Russian"
            else -> "English"
        }
    }
    
    /**
     * Check if two languages are the same
     */
    fun isSameLanguage(lang1: String, lang2: String): Boolean {
        return lang1.equals(lang2, ignoreCase = true)
    }
    
    private fun isArabic(text: String): Boolean {
        val arabicPattern = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]")
        return arabicPattern.containsMatchIn(text)
    }
    
    private fun isJapanese(text: String): Boolean {
        val japanesePattern = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]")
        return japanesePattern.containsMatchIn(text)
    }
    
    private fun isKorean(text: String): Boolean {
        val koreanPattern = Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]")
        return koreanPattern.containsMatchIn(text)
    }
    
    private fun isChinese(text: String): Boolean {
        val chinesePattern = Regex("[\\u4E00-\\u9FFF]")
        return chinesePattern.containsMatchIn(text)
    }
    
    private fun isRussian(text: String): Boolean {
        val russianPattern = Regex("[\\u0400-\\u04FF]")
        return russianPattern.containsMatchIn(text)
    }
}