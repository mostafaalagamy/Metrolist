package com.metrolist.music.betterlyrics

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
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            
            // Find all <p> elements (paragraphs/lines)
            val pElements = doc.getElementsByTagName("p")
            
            for (i in 0 until pElements.length) {
                val pElement = pElements.item(i) as? Element ?: continue
                
                val begin = pElement.getAttribute("begin")
                if (begin.isNullOrEmpty()) continue
                
                val startTime = parseTime(begin)
                val words = mutableListOf<ParsedWord>()
                val lineText = StringBuilder()
                
                // Parse <span> elements (words)
                val spans = pElement.getElementsByTagName("span")
                for (j in 0 until spans.length) {
                    val span = spans.item(j) as? Element ?: continue
                    
                    val wordBegin = span.getAttribute("begin")
                    val wordEnd = span.getAttribute("end")
                    val wordText = span.textContent.trim()
                    
                    if (wordText.isNotEmpty()) {
                        if (lineText.isNotEmpty()) {
                            lineText.append(" ")
                        }
                        lineText.append(wordText)
                        
                        if (wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                            words.add(
                                ParsedWord(
                                    text = wordText,
                                    startTime = parseTime(wordBegin),
                                    endTime = parseTime(wordEnd)
                                )
                            )
                        }
                    }
                }
                
                // If no spans found, use text content directly
                if (lineText.isEmpty()) {
                    lineText.append(pElement.textContent.trim())
                }
                
                if (lineText.isNotEmpty()) {
                    lines.add(
                        ParsedLine(
                            text = lineText.toString(),
                            startTime = startTime,
                            words = words
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Return empty list on parse error
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
                
                // Add word-level timestamps as special comments if available
                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordsData>")
                }
            }
        }
    }
    
    private fun parseTime(timeStr: String): Double {
        // Parse TTML time format (e.g., "9.731", "1:23.456", "1:23:45.678")
        return try {
            when {
                timeStr.contains(":") -> {
                    val parts = timeStr.split(":")
                    when (parts.size) {
                        2 -> {
                            // MM:SS.mmm format
                            val minutes = parts[0].toDouble()
                            val seconds = parts[1].toDouble()
                            minutes * 60 + seconds
                        }
                        3 -> {
                            // HH:MM:SS.mmm format
                            val hours = parts[0].toDouble()
                            val minutes = parts[1].toDouble()
                            val seconds = parts[2].toDouble()
                            hours * 3600 + minutes * 60 + seconds
                        }
                        else -> timeStr.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> timeStr.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
