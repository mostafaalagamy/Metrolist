package com.mostafaalagamy.metrolist.applelyrics.models

import kotlinx.serialization.Serializable

typealias SearchResponse = List<Track>

@Serializable
data class Track(
    val id: String,
    val songName: String,
    val artistName: String,
    val albumName: String? = null,
    val artwork: String,
    val url: String,
    val isrc: String? = null,
    val duration: Int? = 0,
)

@Serializable
data class LyricsResponse(
    val content: List<Line>,
    val type: String
)

@Serializable
data class Line(
    val timestamp: Int,
    val text: List<Syllable>,
    val oppositeTurn: Boolean? = false,
    val background: Boolean? = false,
    val backgroundText: List<Syllable>? = null
)

@Serializable
data class Syllable(
    val timestamp: Int,
    val text: String,
    val part: Boolean? = false,
    val endtime: Int? = null
)
