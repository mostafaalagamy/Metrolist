package com.metrolist.music.translation.utils

/**
 * Post-processing utility for translations
 */
object TranslationPostProcessor {
    
    /**
     * Process and clean up the translated text
     */
    fun process(translatedText: String, targetLanguage: String): String {
        var result = translatedText.trim()
        
        // Language-specific post-processing
        result = when (targetLanguage) {
            "Arabic" -> processArabic(result)
            "English" -> processEnglish(result)
            "French" -> processFrench(result)
            "German" -> processGerman(result)
            "Spanish" -> processSpanish(result)
            else -> result
        }
        
        // General cleanup
        result = generalCleanup(result)
        
        return result
    }
    
    private fun processArabic(text: String): String {
        var result = text
        
        // Fix Arabic punctuation
        result = result.replace(".", ".")
        result = result.replace(",", "،")
        result = result.replace("?", "؟")
        result = result.replace("!", "!")
        
        // Remove extra spaces before punctuation
        result = result.replace(" ،", "،")
        result = result.replace(" ؟", "؟")
        result = result.replace(" !", "!")
        
        return result
    }
    
    private fun processEnglish(text: String): String {
        var result = text
        
        // Capitalize first letter of sentences
        result = result.split(". ").joinToString(". ") { sentence ->
            if (sentence.isNotEmpty()) {
                sentence.first().uppercaseChar() + sentence.drop(1)
            } else sentence
        }
        
        // Capitalize first letter of the entire text
        if (result.isNotEmpty()) {
            result = result.first().uppercaseChar() + result.drop(1)
        }
        
        return result
    }
    
    private fun processFrench(text: String): String {
        var result = text
        
        // French-specific adjustments
        result = result.replace(" :", " :")
        result = result.replace(" ;", " ;")
        result = result.replace(" !", " !")
        result = result.replace(" ?", " ?")
        
        return result
    }
    
    private fun processGerman(text: String): String {
        var result = text
        
        // German-specific capitalization for nouns can be complex
        // For now, just basic cleanup
        return result
    }
    
    private fun processSpanish(text: String): String {
        var result = text
        
        // Spanish-specific punctuation
        result = result.replace("¿", "¿")
        result = result.replace("¡", "¡")
        
        return result
    }
    
    private fun generalCleanup(text: String): String {
        var result = text
        
        // Remove excessive whitespace
        result = result.replace(Regex("\\s+"), " ")
        
        // Remove leading/trailing whitespace
        result = result.trim()
        
        // Fix common punctuation issues
        result = result.replace(" .", ".")
        result = result.replace(" ,", ",")
        result = result.replace("( ", "(")
        result = result.replace(" )", ")")
        
        // Remove quotes if they wrap the entire text
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1)
        }
        
        return result
    }
}