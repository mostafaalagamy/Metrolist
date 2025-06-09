package com.metrolist.music.lyrics

import android.text.format.DateUtils
import com.atilika.kuromoji.ipadic.Tokenizer
import com.metrolist.music.ui.component.ANIMATE_SCROLL_DURATION

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

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

    fun katakanaToRomaji(katakana: String?): String {
        if (katakana == null) return ""

        var romaji: String? = katakana

        romaji = romaji!!.replace("キャ".toRegex(), "kya").replace("キュ".toRegex(), "kyu")
            .replace("キョ".toRegex(), "kyo")
        romaji = romaji.replace("シャ".toRegex(), "sha").replace("シュ".toRegex(), "shu")
            .replace("ショ".toRegex(), "sho")
        romaji = romaji.replace("チャ".toRegex(), "cha").replace("チュ".toRegex(), "chu")
            .replace("チョ".toRegex(), "cho")
        romaji = romaji.replace("ニャ".toRegex(), "nya").replace("ニュ".toRegex(), "nyu")
            .replace("ニョ".toRegex(), "nyo")
        romaji = romaji.replace("ヒャ".toRegex(), "hya").replace("ヒュ".toRegex(), "hyu")
            .replace("ヒョ".toRegex(), "hyo")
        romaji = romaji.replace("ミャ".toRegex(), "mya").replace("ミュ".toRegex(), "myu")
            .replace("ミョ".toRegex(), "myo")
        romaji = romaji.replace("リャ".toRegex(), "rya").replace("リュ".toRegex(), "ryu")
            .replace("リョ".toRegex(), "ryo")
        romaji = romaji.replace("ギャ".toRegex(), "gya").replace("ギュ".toRegex(), "gyu")
            .replace("ギョ".toRegex(), "gyo")
        romaji = romaji.replace("ジャ".toRegex(), "ja").replace("ジュ".toRegex(), "ju")
            .replace("ジョ".toRegex(), "jo")
        romaji = romaji.replace("ビャ".toRegex(), "bya").replace("ビュ".toRegex(), "byu")
            .replace("ビョ".toRegex(), "byo")
        romaji = romaji.replace("ピャ".toRegex(), "pya").replace("ピュ".toRegex(), "pyu")
            .replace("ピョ".toRegex(), "pyo")

        romaji = romaji.replace("ア".toRegex(), "a").replace("イ".toRegex(), "i")
            .replace("ウ".toRegex(), "u")
            .replace("エ".toRegex(), "e").replace("オ".toRegex(), "o")
        romaji = romaji.replace("カ".toRegex(), "ka").replace("キ".toRegex(), "ki")
            .replace("ク".toRegex(), "ku")
            .replace("ケ".toRegex(), "ke").replace("コ".toRegex(), "ko")
        romaji = romaji.replace("サ".toRegex(), "sa").replace("シ".toRegex(), "shi")
            .replace("ス".toRegex(), "su")
            .replace("セ".toRegex(), "se").replace("ソ".toRegex(), "so")
        romaji = romaji.replace("タ".toRegex(), "ta").replace("チ".toRegex(), "chi")
            .replace("ツ".toRegex(), "tsu")
            .replace("テ".toRegex(), "te").replace("ト".toRegex(), "to")
        romaji = romaji.replace("ナ".toRegex(), "na").replace("ニ".toRegex(), "ni")
            .replace("ヌ".toRegex(), "nu")
            .replace("ネ".toRegex(), "ne").replace("ノ".toRegex(), "no")
        romaji = romaji.replace("ハ".toRegex(), "ha").replace("ヒ".toRegex(), "hi")
            .replace("フ".toRegex(), "fu")
            .replace("ヘ".toRegex(), "he").replace("ホ".toRegex(), "ho")
        romaji = romaji.replace("マ".toRegex(), "ma").replace("ミ".toRegex(), "mi")
            .replace("ム".toRegex(), "mu")
            .replace("メ".toRegex(), "me").replace("モ".toRegex(), "mo")
        romaji = romaji.replace("ヤ".toRegex(), "ya").replace("ユ".toRegex(), "yu")
            .replace("ヨ".toRegex(), "yo")
        romaji = romaji.replace("ラ".toRegex(), "ra").replace("リ".toRegex(), "ri")
            .replace("ル".toRegex(), "ru")
            .replace("レ".toRegex(), "re").replace("ロ".toRegex(), "ro")
        romaji = romaji.replace("ワ".toRegex(), "wa").replace("ヲ".toRegex(), "o")
            .replace("ン".toRegex(), "n")
        romaji = romaji.replace("ガ".toRegex(), "ga").replace("ギ".toRegex(), "gi")
            .replace("グ".toRegex(), "gu")
            .replace("ゲ".toRegex(), "ge").replace("ゴ".toRegex(), "go")
        romaji = romaji.replace("ザ".toRegex(), "za").replace("ジ".toRegex(), "ji")
            .replace("ズ".toRegex(), "zu")
            .replace("ゼ".toRegex(), "ze").replace("ゾ".toRegex(), "zo")
        romaji = romaji.replace("ダ".toRegex(), "da").replace("ヂ".toRegex(), "ji")
            .replace("ヅ".toRegex(), "zu")
            .replace("デ".toRegex(), "de").replace("ド".toRegex(), "do")
        romaji = romaji.replace("バ".toRegex(), "ba").replace("ビ".toRegex(), "bi")
            .replace("ブ".toRegex(), "bu")
            .replace("ベ".toRegex(), "be").replace("ボ".toRegex(), "bo")
        romaji = romaji.replace("パ".toRegex(), "pa").replace("ピ".toRegex(), "pi")
            .replace("プ".toRegex(), "pu")
            .replace("ペ".toRegex(), "pe").replace("ポ".toRegex(), "po")

        romaji = romaji.replace("ー".toRegex(), "")

        return romaji.lowercase()
    }

    fun romanizeJapanese(text: String): String {
        val tokenizer = Tokenizer()
        val tokens = tokenizer.tokenize(text)

        return tokens.joinToString(" ") { token ->
            val reading = token.reading
            val kana = if (reading == null || reading == "*") token.surface else reading
            katakanaToRomaji(kana)
        }
    }
    fun isJapanese(text: String): Boolean {
        return text.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' || it in '\u4E00'..'\u9FAF' }
    }
}
