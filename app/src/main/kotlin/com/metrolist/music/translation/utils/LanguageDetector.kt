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
        
        // More accurate detection - check for specific language patterns
        val cleanText = text.trim().lowercase()
        
        return when {
            isArabic(text) -> "Arabic"
            isJapanese(text) -> "Japanese"
            isKorean(text) -> "Korean"
            isChinese(text) -> "Chinese"
            isRussian(text) -> "Russian"
            isFrench(cleanText) -> "French"
            isSpanish(cleanText) -> "Spanish"
            isGerman(cleanText) -> "German"
            isItalian(cleanText) -> "Italian"
            isPortuguese(cleanText) -> "Portuguese"
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
    
    private fun isFrench(text: String): Boolean {
        val frenchWords = setOf("le", "la", "les", "de", "du", "des", "un", "une", "et", "est", "avec", "dans", "pour", "sur", "par", "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "ce", "cette", "ces", "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses")
        val words = text.split("\\s+".toRegex())
        val frenchWordCount = words.count { it in frenchWords }
        return frenchWordCount > words.size * 0.3
    }
    
    private fun isSpanish(text: String): Boolean {
        val spanishWords = setOf("el", "la", "los", "las", "de", "del", "y", "es", "en", "con", "por", "para", "que", "se", "no", "un", "una", "yo", "tú", "él", "ella", "nosotros", "vosotros", "ellos", "ellas", "este", "esta", "estos", "estas", "mi", "mis", "tu", "tus", "su", "sus")
        val words = text.split("\\s+".toRegex())
        val spanishWordCount = words.count { it in spanishWords }
        return spanishWordCount > words.size * 0.3
    }
    
    private fun isGerman(text: String): Boolean {
        val germanWords = setOf("der", "die", "das", "den", "dem", "des", "und", "ist", "in", "mit", "für", "auf", "von", "zu", "ich", "du", "er", "sie", "es", "wir", "ihr", "sie", "ein", "eine", "einen", "einem", "einer", "eines", "mein", "meine", "meinen", "meinem", "meiner", "meines")
        val words = text.split("\\s+".toRegex())
        val germanWordCount = words.count { it in germanWords }
        return germanWordCount > words.size * 0.3
    }
    
    private fun isItalian(text: String): Boolean {
        val italianWords = setOf("il", "la", "lo", "gli", "le", "di", "da", "e", "è", "in", "con", "per", "su", "tra", "che", "si", "non", "un", "una", "io", "tu", "lui", "lei", "noi", "voi", "loro", "questo", "questa", "questi", "queste", "mio", "mia", "miei", "mie", "tuo", "tua", "tuoi", "tue", "suo", "sua", "suoi", "sue")
        val words = text.split("\\s+".toRegex())
        val italianWordCount = words.count { it in italianWords }
        return italianWordCount > words.size * 0.3
    }
    
    private fun isPortuguese(text: String): Boolean {
        val portugueseWords = setOf("o", "a", "os", "as", "de", "do", "da", "dos", "das", "e", "é", "em", "com", "para", "por", "que", "se", "não", "um", "uma", "eu", "tu", "ele", "ela", "nós", "vós", "eles", "elas", "este", "esta", "estes", "estas", "meu", "minha", "meus", "minhas", "teu", "tua", "teus", "tuas", "seu", "sua", "seus", "suas")
        val words = text.split("\\s+".toRegex())
        val portugueseWordCount = words.count { it in portugueseWords }
        return portugueseWordCount > words.size * 0.3
    }
}