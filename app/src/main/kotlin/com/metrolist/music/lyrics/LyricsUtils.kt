package com.metrolist.music.lyrics

import android.text.format.DateUtils
import com.atilika.kuromoji.ipadic.Tokenizer
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

    private val RUSSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        // Special Sequences
        "ого" to "ovo", "Ого" to "Ovo", "его" to "yevo", "Его" to "Yevo",
        // Uppercase Russian to Latin
        "А" to "A", "Б" to "B", "В" to "V", "Г" to "G", "Д" to "D",
        "Е" to "E", "Ё" to "Yo", "Ж" to "Zh", "З" to "Z", "И" to "I",
        "Й" to "Y", "К" to "K", "Л" to "L", "М" to "M", "Н" to "N",
        "О" to "O", "П" to "P", "Р" to "R", "С" to "S", "Т" to "T",
        "У" to "U", "Ф" to "F", "Х" to "Kh", "Ц" to "Ts", "Ч" to "Ch",
        "Ш" to "Sh", "Щ" to "Shch", "Ъ" to "", "Ы" to "Y", "Ь" to "'",
        "Э" to "E", "Ю" to "Yu", "Я" to "Ya",
        // Lowercase Russian to Latin
        "а" to "a", "б" to "b", "в" to "v", "г" to "g", "д" to "d",
        "е" to "e", "ё" to "yo", "ж" to "zh", "з" to "z", "и" to "i",
        "й" to "y", "к" to "k", "л" to "l", "м" to "m", "н" to "n",
        "о" to "o", "п" to "p", "р" to "r", "с" to "s", "т" to "t",
        "у" to "u", "ф" to "f", "х" to "kh", "ц" to "ts", "ч" to "ch",
        "ш" to "sh", "щ" to "shch", "ъ" to "", "ы" to "y", "ь" to "'",
        "э" to "e", "ю" to "yu", "я" to "ya"
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
            if (lines[index].time >= position + 300L) { // Use constant instead of import
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

    suspend fun romanizeRussian(text: String): String = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext ""

        val romajiBuilder = StringBuilder(text.length)
        // Split text into words, preserving delimiters (spaces, punctuation)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { index, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                // Preserve punctuation or spaces as is
                romajiBuilder.append(word)
            } else {
                // Process word
                var charIndex = 0
                while (charIndex < word.length) {
                    var consumed = false
                    // Check for 3-character trigraphs first (e.g., "ого", "его")
                    if (charIndex + 2 < word.length) {
                        val threeCharCandidate = word.substring(charIndex, charIndex + 3)
                        val mappedThreeChar = RUSSIAN_ROMAJI_MAP[threeCharCandidate]
                        if (mappedThreeChar != null) {
                            romajiBuilder.append(mappedThreeChar)
                            charIndex += 3
                            consumed = true
                        }
                    }
                    // Check for 2-character digraphs (e.g., "нь", "Нь")
                    if (!consumed && charIndex + 1 < word.length) {
                        val twoCharCandidate = word.substring(charIndex, charIndex + 2)
                        val mappedTwoChar = RUSSIAN_ROMAJI_MAP[twoCharCandidate]
                        if (mappedTwoChar != null) {
                            romajiBuilder.append(mappedTwoChar)
                            charIndex += 2
                            consumed = true
                        }
                    }
                    // Process single character
                    if (!consumed) {
                        val charStr = word[charIndex].toString()
                        if ((charStr == "е" || charStr == "Е") && charIndex == 0) {
                            // Special case: 'е' or 'Е' at start of word
                            romajiBuilder.append(if (charStr == "е") "ye" else "Ye")
                        } else {
                            // Use standard mapping
                            romajiBuilder.append(RUSSIAN_ROMAJI_MAP[charStr] ?: charStr)
                        }
                        charIndex += 1
                    }
                }
            }
        }

        romajiBuilder.toString()
    }

    fun isRussian(text: String): Boolean {
        return text.any { char ->
            char in '\u0400'..'\u04FF' // Cyrillic Unicode range
        }
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
}
