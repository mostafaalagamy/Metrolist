package com.metrolist.lrclib.models

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class Track(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val duration: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)

internal fun List<Track>.bestMatchingFor(track: Track, duration: Int) = firstOrNull { abs(it.duration - duration) < 2 }
