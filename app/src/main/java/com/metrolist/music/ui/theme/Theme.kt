package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
import com.materialkolor.dynamicColorScheme

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = remember(darkTheme, pureBlack, themeColor) {
        if (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use system dynamic colors if using default theme color on Android 12+
            if (darkTheme) dynamicDarkColorScheme(context).pureBlack(pureBlack)
            else dynamicLightColorScheme(context)
        } else {
            // Use MaterialKolor to generate color scheme from primary color
            // No need for separate pureBlack function call since MaterialKolor supports isAmoled
            dynamicColorScheme(
                primary = themeColor,
                isDark = darkTheme,
                isAmoled = darkTheme && pureBlack,
                style = PaletteStyle.TonalSpot
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val palette = Palette.from(this)
        .maximumColorCount(8)
        .generate()
    // Try to find a vibrant color first, then dominant
    val dominantSwatch = palette.vibrantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.dominantSwatch

    return if (dominantSwatch != null) {
        Color(dominantSwatch.rgb)
    } else {
        // Fall back to the most populous swatch or default color
        palette.swatches
            .maxByOrNull { it.population }
            ?.let { Color(it.rgb) }
            ?: DefaultThemeColor
    }
}

fun Bitmap.extractGradientColors(): List<Color> {
    val palette = Palette.from(this)
        .maximumColorCount(16)
        .generate()
        // Extract and sort swatches by luminance for a pleasing gradient
    val sortedSwatches = palette.swatches
        .asSequence()
        .map { Color(it.rgb) }
        // Filter out colors that are too gray/desaturated
        .filter { color ->
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color.toArgb(), hsv)
            hsv[1] > 0.2f // Only include colors with saturation > 20%
        }
        .sortedByDescending { it.luminance() }
        .toList()

    return when {
        sortedSwatches.size >= 2 -> listOf(sortedSwatches[0], sortedSwatches[1])
        sortedSwatches.size == 1 -> listOf(sortedSwatches[0], Color(0xFF0D0D0D))
        else -> listOf(Color(0xFF595959), Color(0xFF0D0D0D)) // Fallback gradient
    }
}

// This function is no longer needed as MaterialKolor handles AMOLED black directly
// Keeping it for backward compatibility with other parts of the codebase

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
