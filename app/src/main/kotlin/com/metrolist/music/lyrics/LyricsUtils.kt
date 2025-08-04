package com.metrolist.music.lyrics

import android.text.format.DateUtils
import com.atilika.kuromoji.ipadic.Tokenizer
import com.metrolist.music.ui.component.ANIMATE_SCROLL_DURATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(
        // Digraphs (Yōon - combinations like kya, sho)
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo", // ヂ variants, also commonly 'ja', 'ju', 'jo'
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",

        // Basic Katakana Characters
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヲ" to "o", // ヲ is pronounced 'o'
        "ン" to "n",

        // Dakuten (voiced consonants)
        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do", // ヂ and ヅ are often 'ji' and 'zu'

        // Handakuten (p-sounds for 'h' group) / Dakuten for 'h' group
        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo", // Dakuten for ハ행 (ha-row)
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po", // Handakuten for ハ행 (ha-row)

        // Chōonpu (long vowel mark) - removed as per original logic
        "ー" to ""
    )

    private val HANGUL_ROMAJA_MAP: Map<String, Map<String, String>> = mapOf(
        "cho" to mapOf(
            "ᄀ" to "g",  "ᄁ" to "kk", "ᄂ" to "n",  "ᄃ" to "d", 
            "ᄄ" to "tt", "ᄅ" to "r",  "ᄆ" to "m",  "ᄇ" to "b",
            "ᄈ" to "pp", "ᄉ" to "s",  "ᄊ" to "ss", "ᄋ" to "",
            "ᄌ" to "j",  "ᄍ" to "jj", "ᄎ" to "ch", "ᄏ" to "k",
            "ᄐ" to "t",  "ᄑ" to "p",  "ᄒ" to "h"
        ),
        "jung" to mapOf(
            "ᅡ" to "a",  "ᅢ" to "ae", "ᅣ" to "ya",  "ᅤ" to "yae", 
            "ᅥ" to "eo", "ᅦ" to "e",  "ᅧ" to "yeo", "ᅨ" to "ye", 
            "ᅩ" to "o",  "ᅪ" to "wa", "ᅫ" to "wae", "ᅬ" to "oe",
            "ᅭ" to "yo", "ᅮ" to "u",  "ᅯ" to "wo",  "ᅰ" to "we",
            "ᅱ" to "wi", "ᅲ" to "yu", "ᅳ" to "eu",  "ᅴ" to "eui",
            "ᅵ" to "i"
        ),
        "jong" to mapOf(
            "ᆨ" to "k",     "ᆨᄋ" to "g",   "ᆨᄂ" to "ngn", "ᆨᄅ" to "ngn", "ᆨᄆ" to "ngm", "ᆨᄒ" to "kh",
            "ᆩ" to "kk",    "ᆩᄋ" to "kg",  "ᆩᄂ" to "ngn", "ᆩᄅ" to "ngn", "ᆩᄆ" to "ngm", "ᆩᄒ" to "kh",
            "ᆪ" to "k",     "ᆪᄋ" to "ks",  "ᆪᄂ" to "ngn", "ᆪᄅ" to "ngn", "ᆪᄆ" to "ngm", "ᆪᄒ" to "kch",
            "ᆫ" to "n",     "ᆫᄅ" to "ll",  "ᆬ" to "n",     "ᆬᄋ" to "nj",  "ᆬᄂ" to "nn",  "ᆬᄅ" to "nn",
            "ᆬᄆ" to "nm",  "ᆬㅎ" to "nch", "ᆭ" to "n",     "ᆭᄋ" to "nh",  "ᆭᄅ" to "nn",  "ᆮ" to "t",
            "ᆮᄋ" to "d",   "ᆮᄂ" to "nn",  "ᆮᄅ" to "nn",  "ᆮᄆ" to "nm",  "ᆮᄒ" to "th",  "ᆯ" to "l",
            "ᆯᄋ" to "r",   "ᆯᄂ" to "ll",  "ᆯᄅ" to "ll",  "ᆰ" to "k",     "ᆰᄋ" to "lg",  "ᆰᄂ" to "ngn",
            "ᆰᄅ" to "ngn", "ᆰᄆ" to "ngm", "ᆰᄒ" to "lkh", "ᆱ" to "m",     "ᆱᄋ" to "lm",  "ᆱᄂ" to "mn",
            "ᆱᄅ" to "mn",  "ᆱᄆ" to "mm",  "ᆱᄒ" to "lmh", "ᆲ" to "p",     "ᆲᄋ" to "lb",  "ᆲᄂ" to "mn",
            "ᆲᄅ" to "mn",  "ᆲᄆ" to "mm",  "ᆲᄒ" to "lph", "ᆳ" to "t",     "ᆳᄋ" to "ls",  "ᆳᄂ" to "nn",
            "ᆳᄅ" to "nn",  "ᆳᄆ" to "nm",  "ᆳᄒ" to "lsh", "ᆴ" to "t",     "ᆴᄋ" to "lt",  "ᆴᄂ" to "nn",
            "ᆴᄅ" to "nn",  "ᆴᄆ" to "nm",  "ᆴᄒ" to "lth", "ᆵ" to "p",     "ᆵᄋ" to "lp",  "ᆵᄂ" to "mn",
            "ᆵᄅ" to "mn",  "ᆵᄆ" to "mm",  "ᆵᄒ" to "lph", "ᆶ" to "l",     "ᆶᄋ" to "lh",  "ᆶᄂ" to "ll",
            "ᆶᄅ" to "ll",  "ᆶᄆ" to "lm",  "ᆶᄒ" to "lh",  "ᆷ" to "m",     "ᆷᄅ" to "mn",  "ᆸ" to "p",
            "ᆸᄋ" to "b",   "ᆸᄂ" to "mn",  "ᆸᄅ" to "mn",  "ᆸᄆ" to "mm",  "ᆸᄒ" to "ph",  "ᆹ" to "p",
            "ᆹᄋ" to "ps",  "ᆹᄂ" to "mn",  "ᆹᄅ" to "mn",  "ᆹᄆ" to "mm",  "ᆹᄒ" to "psh", "ᆺ" to "t",
            "ᆺᄋ" to "s",   "ᆺᄂ" to "nn",  "ᆺᄅ" to "nn",  "ᆺᄆ" to "nm",  "ᆺᄒ" to "sh",  "ᆻ" to "t",
            "ᆻᄋ" to "ss",  "ᆻᄂ" to "tn",  "ᆻᄅ" to "tn",  "ᆻᄆ" to "nm",  "ᆻᄒ" to "th",  "ᆼ" to "ng",
            "ᆽ" to "t",     "ᆽᄋ" to "j",   "ᆽᄂ" to "nn",  "ᆽᄅ" to "nn",  "ᆽᄆ" to "nm",  "ᆽᄒ" to "ch",
            "ᆾ" to "t",     "ᆾᄋ" to "ch",  "ᆾᄂ" to "nn",  "ᆾᄅ" to "nn",  "ᆾᄆ" to "nm",  "ᆾᄒ" to "ch",
            "ᆿ" to "k",     "ᆿᄋ" to "k",   "ᆿᄂ" to "ngn", "ᆿᄅ" to "ngn", "ᆿᄆ" to "ngm", "ᆿᄒ" to "kh",
            "ᇀ" to "t",     "ᇀᄋ" to "t",   "ᇀᄂ" to "nn",  "ᇀᄅ" to "nn",  "ᇀᄆ" to "nm",  "ᇀᄒ" to "th",
            "ᇁ" to "p",     "ᇁᄋ" to "p",   "ᇁᄂ" to "mn",  "ᇁᄅ" to "mn",  "ᇁᄆ" to "mm",  "ᇁᄒ" to "ph",
            "ᇂ" to "t",     "ᇂᄋ" to "h",   "ᇂᄂ" to "nn",  "ᇂᄅ" to "nn",  "ᇂᄆ" to "mm",  "ᇂᄒ" to "t",
            "ᇂᄀ" to "k",
        )
    )

    // Lazy initialized Tokenizer
    private val kuromojiTokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    fun parseLyrics(lyrics: String): List<LyricsEntry> =
        lyrics
            .lines()
            .flatMap { line ->
                parseLine(line).orEmpty()
            }.sorted()

    private fun parseLine(line: String): List<LyricsEntry>? {
        if (line.isEmpty()) {
            return null
        }
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        val text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        for (index in lines.indices) {
            if (lines[index].time >= position + ANIMATE_SCROLL_DURATION) {
                return index - 1
            }
        }
        return lines.lastIndex
    }

    /**
     * Converts a Katakana string to Romaji.
     * This optimized version uses a pre-defined map and StringBuilder for better performance
     * compared to chained regex replacements.
     * Expected impact: Significant reduction in object creation (Regex, String) and faster execution.
     */
    fun katakanaToRomaji(katakana: String?): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length) // Initial capacity
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            // Prioritize 2-character sequences from the map (e.g., "キャ" before "キ")
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                // If no 2-character sequence matched, try 1-character
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    // If the character is not in Katakana map, append it as is.
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    /**
     * Romanizes Japanese text using Kuromoji Tokenizer and the optimized katakanaToRomaji function.
     * Runs on Dispatchers.Default for CPU-intensive work.
     * Expected impact: Faster tokenization due to reused Tokenizer instance and faster
     * per-token romanization.
     */
    suspend fun romanizeJapanese(text: String): String = withContext(Dispatchers.Default) {
        // Use the lazily initialized tokenizer
        val tokens = kuromojiTokenizer.tokenize(text)

        val romanizedTokens = tokens.mapIndexed { index, token ->
            val currentReading = if (token.reading.isNullOrEmpty() || token.reading == "*") {
                token.surface
            } else {
                token.reading
            }

            // Pass the next token's reading for sokuon handling if applicable
            val nextTokenReading = if (index + 1 < tokens.size) {
                tokens[index + 1].reading?.takeIf { it.isNotEmpty() && it != "*" } ?: tokens[index + 1].surface
            } else {
                null
            }
            katakanaToRomaji(currentReading, nextTokenReading)
        }
        romanizedTokens.joinToString(" ")
    }

    /**
     * Converts a Katakana string to Romaji.
     * This optimized version uses a pre-defined map and StringBuilder for better performance
     * compared to chained regex replacements.
     * Expected impact: Significant reduction in object creation (Regex, String) and faster execution.
     * @param katakana The Katakana string to convert.
     * @param nextKatakana Optional: The next Katakana string (from the next token) to help with sokuon (ッ) gemination.
     */
    fun katakanaToRomaji(katakana: String?, nextKatakana: String? = null): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length) // Initial capacity
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            // Prioritize 2-character sequences from the map (e.g., "キャ" before "キ")
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            // Handle sokuon (ッ) - gemination
            if (!consumed && katakana[i] == 'ッ') {
                val nextCharToDouble = nextKatakana?.getOrNull(0)
                if (nextCharToDouble != null) {
                    val nextCharRomaji = KANA_ROMAJI_MAP[nextCharToDouble.toString()]?.getOrNull(0)?.toString()
                        ?: nextCharToDouble.toString()
                    romajiBuilder.append(nextCharRomaji.lowercase().trim())
                }
                // Sokuon itself doesn't have a direct romaji representation other than geminating the next consonant.
                // We just consume 'ッ' and let the next character (if any within the current token) be processed normally.
                i += 1 // Consume the 'ッ'
                consumed = true
            }

            if (!consumed) {
                // If no 2-character sequence matched, try 1-character
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    // If the character is not in Katakana map, append it as is.
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    suspend fun romanizeKorean(text: String): String = withContext(Dispatchers.Default) {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null

        for (i in text.indices) {
            val char = text[i]

            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey)
                        ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal)
                        ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)

                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }

        if (prevFinal != null) {
            val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
            romajaBuilder.append(jong)
        }

        romajaBuilder.toString()
    }

    /**
     * Checks if the given text contains any Japanese characters (Hiragana, Katakana, or common Kanji).
     * This function is generally efficient due to '.any' and early exit.
     * No major performance bottlenecks expected here for typical inputs.
     */
    fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') || // Hiragana
            (char in '\u30A0'..'\u30FF') || // Katakana
            // CJK Unified Ideographs (covers most common Kanji)
            // Note: This range also includes many Chinese Hanzi.
            // Differentiating Japanese Kanji from Chinese Hanzi solely based on Unicode
            // ranges is challenging as they share many characters.
            // For more accurate Japanese detection, one might need to analyze
            // the presence of Hiragana/Katakana alongside Kanji.
            (char in '\u4E00'..'\u9FFF')
        }
    }

    /**
     * Checks if the given text contains any Korean characters (Hangul Syllables, Jamo, etc.).
     */
    fun isKorean(text: String): Boolean {
        return text.any { char ->
            (char in '\uAC00'..'\uD7A3') // Hangul Syllables
        }
    }
        
    /**
     * Checks if the given text contains any Chinese characters (common Hanzi).
     * This function is generally efficient due to '.any' and early exit.
     * To improve accuracy in distinguishing between Chinese and Japanese (which shares Kanji),
     * this function now checks if the text *predominantly* consists of CJK Unified Ideographs
     * and *lacks* significant amounts of Hiragana or Katakana.
     *
     * A simple threshold is used here. More sophisticated methods (e.g., frequency analysis,
     * dictionaries, or machine learning models) would be needed for higher accuracy.
     */
    fun isChinese(text: String): Boolean {
        if (text.isEmpty()) return false

        val cjkCharCount = text.count { char -> char in '\u4E00'..'\u9FFF' }
        val hiraganaKatakanaCount = text.count { char -> (char in '\u3040'..'\u309F') || (char in '\u30A0'..'\u30FF') }

        // Heuristic: If CJK characters are present and there are very few or no Hiragana/Katakana,
        // it's more likely to be Chinese.
        // The threshold (e.g., 0.1) can be adjusted based on desired sensitivity.
        return cjkCharCount > 0 && (hiraganaKatakanaCount.toDouble() / text.length.toDouble()) < 0.1
    }

    /**
     * Detects if text is in Arabic
     */
    fun isArabic(text: String): Boolean {
        if (text.isEmpty()) return false
        
        val arabicCharCount = text.count { char -> char in '\u0600'..'\u06FF' || char in '\u0750'..'\u077F' }
        val totalChars = text.replace(" ", "").length
        
        // If more than 30% of non-space characters are Arabic, consider it Arabic text
        return if (totalChars > 0) {
            (arabicCharCount.toDouble() / totalChars.toDouble()) > 0.3
        } else false
    }

    /**
     * Detects if text needs translation based on language detection
     */
    fun needsTranslation(text: String, targetLanguage: String = "English"): Boolean {
        return com.metrolist.music.translation.TranslationService.needsTranslation(text, targetLanguage)
    }
    
    /**
     * Simple language detection based on character patterns
     */
    private fun detectLanguage(text: String): String {
        if (text.isEmpty()) return "Unknown"
        
        val cleanText = text.replace(" ", "").lowercase()
        
        // Arabic detection
        val arabicCharCount = text.count { char -> char in '\u0600'..'\u06FF' || char in '\u0750'..'\u077F' }
        if (arabicCharCount > cleanText.length * 0.3) return "Arabic"
        
        // Japanese detection (Hiragana, Katakana, Kanji)
        val japaneseCharCount = text.count { char -> 
            char in '\u3040'..'\u309F' || char in '\u30A0'..'\u30FF' || char in '\u4E00'..'\u9FFF'
        }
        if (japaneseCharCount > 0) return "Japanese"
        
        // Korean detection
        val koreanCharCount = text.count { char -> char in '\uAC00'..'\uD7A3' }
        if (koreanCharCount > 0) return "Korean"
        
        // Chinese detection (mainly Hanzi without Japanese kana)
        val chineseCharCount = text.count { char -> char in '\u4E00'..'\u9FFF' }
        val kanaCount = text.count { char -> char in '\u3040'..'\u309F' || char in '\u30A0'..'\u30FF' }
        if (chineseCharCount > 0 && kanaCount == 0) return "Chinese"
        
        // Cyrillic (Russian and others)
        val cyrillicCharCount = text.count { char -> char in '\u0400'..'\u04FF' }
        if (cyrillicCharCount > cleanText.length * 0.3) return "Russian"
        
        // Default to English for Latin characters
        return "English"
    }

    /**
     * Translates text to user's preferred language using organized translation service
     */
    suspend fun translateLyricsWithAI(text: String, targetLanguage: String = "English"): String = withContext(Dispatchers.IO) {
        try {
            // Use the organized translation service
            return@withContext com.metrolist.music.translation.TranslationService.translateLyricsWithAI(text, targetLanguage)
        } catch (e: Exception) {
            // Return original text if translation fails
            text
        }
    }
    
    /**
     * Improved translation with modern APIs and better context handling for lyrics
     */
    private suspend fun translateWithImprovedContext(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        try {
            // Clean the text first
            val cleanText = text.trim()
            if (cleanText.isEmpty()) return@withContext text
            
            // Skip very short words or common interjections that don't need translation
            if (shouldSkipTranslation(cleanText)) {
                return@withContext text
            }
            
            // For lyrics, we want more natural, poetic translations
            val enhancedText = preprocessLyricsText(cleanText)
            
            // Try multiple translation services in order of preference
            return@withContext translateWithMultipleServices(enhancedText, targetLanguage)
            
        } catch (e: Exception) {
            text
        }
    }
    
    /**
     * Try multiple translation services for better reliability and quality
     */
    private suspend fun translateWithMultipleServices(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val sourceLang = detectLanguage(text)
        val sourceLangCode = getLanguageCode(sourceLang)
        val targetLangCode = getLanguageCode(targetLanguage)
        
        // Don't translate if source and target are the same
        if (sourceLangCode == targetLangCode) {
            return@withContext text
        }
        
        // Try services in order of preference (AI first for better context)
        val translationServices = listOf<suspend () -> String>(
            { translateWithAI(text, sourceLang, targetLanguage) },
            { translateWithLibreTranslate(text, sourceLangCode, targetLangCode) },
            { translateWithMicrosoftTranslator(text, sourceLangCode, targetLangCode) },
            { translateWithGoogleTranslate(text, targetLanguage) }
        )
        
        for (service in translationServices) {
            try {
                val result = service()
                if (result != text && result.isNotBlank()) {
                    return@withContext postProcessTranslation(result, targetLanguage)
                }
            } catch (e: Exception) {
                // Continue to next service
                continue
            }
        }
        
        // If all services fail, return original text
        return@withContext text
    }
    
    /**
     * AI-powered translation with context understanding for song lyrics
     * Uses Hugging Face or similar AI models for better contextual translation
     */
    private suspend fun translateWithAI(text: String, sourceLang: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        try {
            // Enhanced prompt for song lyrics context
            val prompt = buildLyricsTranslationPrompt(text, sourceLang, targetLanguage)
            
            // Try multiple AI services
            val aiServices = listOf<suspend () -> String>(
                { translateWithHuggingFace(prompt, text) },
                { translateWithGroqAI(prompt, text) },
                { translateWithOllamaLocal(prompt, text) }
            )
            
            for (aiService in aiServices) {
                try {
                    val result = aiService()
                    if (result != text && result.isNotBlank() && !result.startsWith("Error")) {
                        return@withContext result
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            throw Exception("All AI services failed")
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Build intelligent prompt for song lyrics translation
     */
    private fun buildLyricsTranslationPrompt(text: String, sourceLang: String, targetLanguage: String): String {
        return """
            You are a professional translator specializing in song lyrics and poetry. 
            
            Task: Translate the following song lyrics from $sourceLang to $targetLanguage.
            
            Instructions:
            - Preserve the emotional meaning and poetic essence
            - Maintain the rhythm and flow where possible
            - Consider cultural context and metaphors
            - Use natural, contemporary language
            - Avoid literal word-for-word translation
            - If it's a name, place, or proper noun, keep it as is
            - If it's a universal expression (oh, ah, wow), adapt appropriately
            
            Original lyrics: "$text"
            
            Provide ONLY the translated text without any explanations:
        """.trimIndent()
    }
    
    /**
     * Hugging Face AI Translation
     */
    private suspend fun translateWithHuggingFace(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            // Using Hugging Face Inference API with a good translation model
            val url = "https://api-inference.huggingface.co/models/facebook/mbart-large-50-many-to-many-mmt"
            
            val requestBody = """
                {
                    "inputs": "$prompt",
                    "parameters": {
                        "max_length": 200,
                        "temperature": 0.7,
                        "do_sample": true
                    }
                }
            """.trimIndent()
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            // Note: For production, add your Hugging Face API key
            // connection.setRequestProperty("Authorization", "Bearer YOUR_HF_TOKEN")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val translatedText = parseHuggingFaceResponse(response)
                return@withContext translatedText ?: originalText
            } else {
                throw Exception("Hugging Face API error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Groq AI Translation (very fast and free)
     */
    private suspend fun translateWithGroqAI(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.groq.com/openai/v1/chat/completions"
            
            val requestBody = """
                {
                    "model": "llama3-8b-8192",
                    "messages": [
                        {
                            "role": "user",
                            "content": "$prompt"
                        }
                    ],
                    "temperature": 0.7,
                    "max_tokens": 150
                }
            """.trimIndent()
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            // Note: For production, add your Groq API key
            // connection.setRequestProperty("Authorization", "Bearer YOUR_GROQ_API_KEY")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val translatedText = parseGroqResponse(response)
                return@withContext translatedText ?: originalText
            } else {
                throw Exception("Groq AI error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Local Ollama AI Translation (if available)
     */
    private suspend fun translateWithOllamaLocal(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "http://localhost:11434/api/generate"
            
            val requestBody = """
                {
                    "model": "llama3.2",
                    "prompt": "$prompt",
                    "stream": false,
                    "options": {
                        "temperature": 0.7,
                        "top_p": 0.9
                    }
                }
            """.trimIndent()
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 45000
            
            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val translatedText = parseOllamaResponse(response)
                return@withContext translatedText ?: originalText
            } else {
                throw Exception("Ollama local AI error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Parse Hugging Face API response
     */
    private fun parseHuggingFaceResponse(response: String): String? {
        try {
            val regex = Regex("\"generated_text\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            return match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse Groq AI response
     */
    private fun parseGroqResponse(response: String): String? {
        try {
            val regex = Regex("\"content\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            return match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse Ollama response
     */
    private fun parseOllamaResponse(response: String): String? {
        try {
            val regex = Regex("\"response\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            return match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * LibreTranslate API - Free and open source translation
     */
    private suspend fun translateWithLibreTranslate(text: String, sourceLang: String, targetLang: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            
            // Use public LibreTranslate instance (you can change this to your own instance)
            val url = "https://libretranslate.de/translate"
            
            val jsonPayload = """
                {
                    "q": "$text",
                    "source": "$sourceLang",
                    "target": "$targetLang",
                    "format": "text"
                }
            """.trimIndent()
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            
            // Send request
            connection.outputStream.use { os ->
                val input = jsonPayload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val translatedText = parseLibreTranslateResponse(response)
                return@withContext translatedText ?: text
            } else {
                throw Exception("LibreTranslate API error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Microsoft Translator API - Free tier available
     */
    private suspend fun translateWithMicrosoftTranslator(text: String, sourceLang: String, targetLang: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            
            // Microsoft Translator Text API v3.0
            val url = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=$sourceLang&to=$targetLang"
            
            val jsonPayload = """
                [{"Text": "$text"}]
            """.trimIndent()
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            // Note: For production, you would need a proper API key here
            // connection.setRequestProperty("Ocp-Apim-Subscription-Key", "YOUR_API_KEY")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            
            // Send request
            connection.outputStream.use { os ->
                val input = jsonPayload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val translatedText = parseMicrosoftTranslatorResponse(response)
                return@withContext translatedText ?: text
            } else {
                throw Exception("Microsoft Translator API error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Parse LibreTranslate JSON response
     */
    private fun parseLibreTranslateResponse(response: String): String? {
        try {
            // LibreTranslate returns: {"translatedText": "translated text"}
            val regex = Regex("\"translatedText\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            
            if (match != null) {
                var translation = match.groupValues[1]
                // Clean up escaped characters
                translation = translation.replace("\\\"", "\"")
                translation = translation.replace("\\n", "\n")
                translation = translation.replace("\\/", "/")
                return translation
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse Microsoft Translator JSON response
     */
    private fun parseMicrosoftTranslatorResponse(response: String): String? {
        try {
            // Microsoft returns: [{"translations":[{"text":"translated text","to":"target_lang"}]}]
            val regex = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            
            if (match != null) {
                var translation = match.groupValues[1]
                // Clean up escaped characters
                translation = translation.replace("\\\"", "\"")
                translation = translation.replace("\\n", "\n")
                translation = translation.replace("\\/", "/")
                return translation
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Check if text should be skipped from translation
     */
    private fun shouldSkipTranslation(text: String): Boolean {
        val cleanText = text.trim().lowercase()
        
        // Skip very short text (less than 2 characters)
        if (cleanText.length < 2) return true
        
        // Skip common musical interjections that are universal
        val universalSounds = listOf("oh", "ah", "eh", "mm", "hmm", "la", "na", "da", "ya", "وو", "آه", "أوه", "لا", "نا", "يا")
        if (universalSounds.contains(cleanText)) return true
        
        // Skip if text is mostly punctuation or numbers
        val alphaCount = cleanText.count { it.isLetter() }
        if (alphaCount < cleanText.length * 0.5) return true
        
        return false
    }
    
    /**
     * Preprocess lyrics text to improve translation quality
     */
    private fun preprocessLyricsText(text: String): String {
        // Remove excessive punctuation that might confuse translation
        var processedText = text.replace(Regex("[.]{2,}"), "...")
        processedText = processedText.replace(Regex("[!]{2,}"), "!")
        processedText = processedText.replace(Regex("[?]{2,}"), "?")
        
        // Handle common lyrical repetitions
        processedText = processedText.replace(Regex("\\b(\\w+)\\s+\\1\\b"), "$1") // Remove word repetitions like "حبيبي حبيبي" -> "حبيبي"
        
        return processedText.trim()
    }
    
    /**
     * Post-process translation specifically for lyrics
     */
    private fun postProcessTranslation(translation: String, targetLanguage: String): String {
        var processedTranslation = translation
        
        // Fix common translation issues for lyrics
        when (targetLanguage) {
            "Arabic" -> {
                // Ensure proper Arabic punctuation
                processedTranslation = processedTranslation.replace("?", "؟")
                processedTranslation = processedTranslation.replace(",", "،")
            }
            "English" -> {
                // Capitalize first letter of sentences
                processedTranslation = processedTranslation.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
            }
        }
        
        // Remove excessive whitespace
        processedTranslation = processedTranslation.replace(Regex("\\s+"), " ").trim()
        
        return processedTranslation
    }
    
    /**
     * Simple translation using Google Translate web service (Enhanced)
     * Note: This is a fallback option
     */
    private suspend fun translateWithGoogleTranslate(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        try {
            val sourceLang = detectLanguage(text)
            val sourceLangCode = getGoogleLanguageCode(sourceLang)
            val targetLangCode = getGoogleLanguageCode(targetLanguage)
            
            // Don't translate if source and target are the same
            if (sourceLangCode == targetLangCode) {
                return@withContext text
            }
            
            val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
            
            // Enhanced URL with more parameters for better translation quality
            val url = "https://translate.googleapis.com/translate_a/single?" +
                    "client=gtx&" +
                    "sl=$sourceLangCode&" +
                    "tl=$targetLangCode&" +
                    "dt=t&" +          // Translation
                    "dt=bd&" +         // Dictionary
                    "dt=qc&" +         // Quality check
                    "dt=rm&" +         // Romanization
                    "q=$encodedText"
            
            // Create connection with better headers
            val connection = java.net.URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.connectTimeout = 10000 // 10 seconds timeout
            connection.readTimeout = 15000    // 15 seconds read timeout
            
            val response = connection.getInputStream().bufferedReader().readText()
            
            // Better JSON parsing for translation
            val translatedText = parseGoogleTranslateResponse(response)
            
            return@withContext translatedText ?: text
            
        } catch (e: Exception) {
            text // Return original text if translation fails
        }
    }
    
    /**
     * Parse Google Translate response more accurately
     */
    private fun parseGoogleTranslateResponse(response: String): String? {
        try {
            // Google Translate returns a complex JSON array
            // Format: [[["translated text","original text",null,null,3]],null,"source_lang"]
            
            val regex = Regex("\"([^\"]+)\"")
            val matches = regex.findAll(response).toList()
            
            if (matches.isNotEmpty()) {
                // Get the first translation segment
                var translation = matches[0].groupValues[1]
                
                // Clean up the translation
                translation = translation.replace("\\n", "\n")
                translation = translation.replace("\\\"", "\"")
                translation = translation.replace("\\/", "/")
                
                return translation
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Convert language name to language code for LibreTranslate/Microsoft Translator
     */
    private fun getLanguageCode(language: String): String {
        return when (language) {
            "Arabic" -> "ar"
            "English" -> "en"
            "French" -> "fr"
            "German" -> "de"
            "Spanish" -> "es"
            "Italian" -> "it"
            "Portuguese" -> "pt"
            "Russian" -> "ru"
            "Chinese" -> "zh"
            "Japanese" -> "ja"
            "Korean" -> "ko"
            else -> "auto" // Let service auto-detect
        }
    }
    
    /**
     * Convert language name to language code specifically for Google Translate
     */
    private fun getGoogleLanguageCode(language: String): String {
        return when (language) {
            "Arabic" -> "ar"
            "English" -> "en"
            "French" -> "fr"
            "German" -> "de"
            "Spanish" -> "es"
            "Italian" -> "it"
            "Portuguese" -> "pt"
            "Russian" -> "ru"
            "Chinese" -> "zh"
            "Japanese" -> "ja"
            "Korean" -> "ko"
            else -> "auto" // Let Google auto-detect
        }
    }
}
