package com.metrolist.music.eq.data

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Represents a single parametric EQ filter/band
 * Supports all AutoEQ filter types
 */
@Serializable
data class ParametricEQBand(
    val frequency: Double,                      // Center frequency in Hz
    val gain: Double,                           // Gain in dB
    val q: Double = 1.41,                       // Q factor (bandwidth) - default to sqrt(2)
    val filterType: FilterType = FilterType.PK, // Filter type
    val enabled: Boolean = true                 // Whether this band is active
)

/**
 * Represents a complete parametric EQ configuration for a headphone
 * Parsed from AutoEQ preset files (ParametricEQ.txt, FixedBandEQ.txt)
 */
@Serializable
data class ParametricEQ(
    val preamp: Double,                         // Preamp/gain in dB (to prevent clipping)
    val bands: List<ParametricEQBand>,          // List of EQ bands
    val metadata: Map<String, String> = emptyMap()  // Additional metadata from file
) {
    companion object {
        const val MAX_BANDS = 20  // Maximum bands supported by the implementation

        // Standard 10-band frequencies used by AutoEQ FixedBandEQ format
        val STANDARD_10_BAND_FREQUENCIES = listOf(
            31.0, 62.0, 125.0, 250.0, 500.0,
            1000.0, 2000.0, 4000.0, 8000.0, 16000.0
        )
    }

    /**
     * Returns a limited version with maximum number of bands
     * (useful for hardware EQs with band limitations)
     */
    fun limitToBands(maxBands: Int): ParametricEQ {
        if (bands.size <= maxBands) return this

        // Sort bands by absolute gain (descending) to keep most impactful bands
        val sortedBands = bands.sortedByDescending { abs(it.gain) }

        return ParametricEQ(
            preamp = preamp,
            bands = sortedBands.take(maxBands),
            metadata = metadata
        )
    }
}