package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import kotlin.math.abs

val DefaultThemeColor = Color(0xFFED5564)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use the defined M3 Expressive Typography
    // TODO: Define M3 Expressive Shapes instance if needed
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Use the defined AppTypography
        // shapes = MaterialTheme.shapes, // Placeholder - Needs update (Shapes not used in original)
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

/**
 * Enhanced gradient color extraction inspired by premium music players.
 * This function extracts more refined gradient colors with better algorithm
 * that considers both vibrant colors and optimal color contrast for gradients.
 */
fun Bitmap.extractGradientColors(): List<Color> {
    val palette = Palette.from(this)
        .maximumColorCount(128) // Increased for better color analysis
        .generate()
    
    // Get all available swatches sorted by population
    val allSwatches = palette.swatches.sortedByDescending { it.population }
    
    // Try to get vibrant colors first (like premium music players)
    val vibrantColor = palette.vibrantSwatch?.rgb
    val darkVibrantColor = palette.darkVibrantSwatch?.rgb
    val lightVibrantColor = palette.lightVibrantSwatch?.rgb
    val mutedColor = palette.mutedSwatch?.rgb
    val darkMutedColor = palette.darkMutedSwatch?.rgb
    val lightMutedColor = palette.lightMutedSwatch?.rgb
    
    // Collect potential colors with their properties
    val candidateColors = mutableListOf<Pair<Color, Double>>()
    
    // Add vibrant colors with high priority
    vibrantColor?.let { candidateColors.add(Color(it) to 1.0) }
    darkVibrantColor?.let { candidateColors.add(Color(it) to 0.9) }
    lightVibrantColor?.let { candidateColors.add(Color(it) to 0.8) }
    
    // Add muted colors with medium priority
    mutedColor?.let { candidateColors.add(Color(it) to 0.7) }
    darkMutedColor?.let { candidateColors.add(Color(it) to 0.6) }
    lightMutedColor?.let { candidateColors.add(Color(it) to 0.5) }
    
    // Add high-population colors if we don't have enough candidates
    if (candidateColors.size < 6) {
        allSwatches.take(8).forEach { swatch ->
            val color = Color(swatch.rgb)
            if (candidateColors.none { abs(it.first.luminance() - color.luminance()) < 0.1f }) {
                candidateColors.add(color to 0.4)
            }
        }
    }
    
    // Select the best gradient pair
    return selectOptimalGradientPair(candidateColors.map { it.first }.distinct())
}

/**
 * Selects the optimal gradient pair from candidate colors based on:
 * - Appropriate luminance contrast for gradient effect
 * - Color harmony
 * - Visual appeal
 */
private fun selectOptimalGradientPair(colors: List<Color>): List<Color> {
    if (colors.size < 2) {
        return listOf(
            Color(0xFF1A1A1A), // Rich dark color
            Color(0xFF0A0A0A)  // Deeper dark color
        )
    }
    
    var bestPair = colors.take(2)
    var bestScore = 0.0
    
    // Test all possible pairs
    for (i in colors.indices) {
        for (j in i + 1 until colors.size) {
            val color1 = colors[i]
            val color2 = colors[j]
            val score = calculateGradientScore(color1, color2)
            
            if (score > bestScore) {
                bestScore = score
                bestPair = listOf(color1, color2)
            }
        }
    }
    
    // Sort by luminance to ensure proper gradient direction
    return bestPair.sortedByDescending { it.luminance() }
}

/**
 * Calculates a score for how good two colors would work as a gradient pair
 */
private fun calculateGradientScore(color1: Color, color2: Color): Double {
    val luminanceDiff = abs(color1.luminance() - color2.luminance())
    
    // Prefer moderate luminance differences (0.15 to 0.6 is ideal for gradients)
    val luminanceScore = when {
        luminanceDiff < 0.1 -> 0.1 // Too similar
        luminanceDiff < 0.15 -> luminanceDiff * 2 // Getting better
        luminanceDiff <= 0.6 -> 1.0 // Perfect range
        luminanceDiff <= 0.8 -> 0.8 // Still good
        else -> 0.3 // Too high contrast
    }
    
    // Prefer colors that aren't too close to pure black or white
    val color1Vibrancy = 1.0 - abs(color1.luminance() - 0.5) * 2
    val color2Vibrancy = 1.0 - abs(color2.luminance() - 0.5) * 2
    val vibrancyScore = (color1Vibrancy + color2Vibrancy) / 2
    
    return luminanceScore * 0.7 + vibrancyScore * 0.3
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
