package com.mostafaalagamy.metrolist.betterlyrics

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {

    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double
    )

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()

        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            val pElements = doc.getElementsByTagName("p")

            for (i in 0 until pElements.length) {
                val pElement = pElements.item(i) as? Element ?: continue
                val begin = pElement.getAttribute("begin").takeIf { it.isNotEmpty() } ?: continue

                val startTime = parseTime(begin)
                val words = mutableListOf<ParsedWord>()
                val lineText = StringBuilder()

                val spans = pElement.getElementsByTagName("span")
                for (j in 0 until spans.length) {
                    val span = spans.item(j) as? Element ?: continue
                    val wordText = span.textContent.trim()

                    if (wordText.isNotEmpty()) {
                        lineText.append(if (lineText.isNotEmpty()) " " else "").append(wordText)

                        val wordBegin = span.getAttribute("begin")
                        val wordEnd = span.getAttribute("end")
                        if (wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                            words.add(ParsedWord(wordText, parseTime(wordBegin), parseTime(wordEnd)))
                        }
                    }
                }

                if (lineText.isEmpty()) {
                    lineText.append(pElement.textContent.trim())
                }

                if (lineText.isNotEmpty()) {
                    lines.add(ParsedLine(lineText.toString(), startTime, words))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return lines
    }

    fun toLRC(lines: List<ParsedLine>): String {
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10

                appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, line.text))

                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { "${it.text}:${it.startTime}:${it.endTime}" }
                    appendLine("<$wordsData>")
                }
            }
        }
    }

    private fun parseTime(timeStr: String): Double {
        return try {
            if (timeStr.contains(":")) {
                val parts = timeStr.split(":")
                when (parts.size) {
                    2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
                    3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
                    else -> 0.0
                }
            } else {
                timeStr.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0
        }
    }
}
