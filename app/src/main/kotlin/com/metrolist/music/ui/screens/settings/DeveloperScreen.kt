@file:OptIn(ExperimentalMaterial3Api::class)

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import com.metrolist.music.constants.SliderStyle
import me.saket.squiggles.SquigglySlider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain

@Composable
fun DeveloperScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val colors = listOf(
        "primary" to MaterialTheme.colorScheme.primary,
            "onPrimary" to MaterialTheme.colorScheme.onPrimary,
            "primaryContainer" to MaterialTheme.colorScheme.primaryContainer,
            "onPrimaryContainer" to MaterialTheme.colorScheme.onPrimaryContainer,
            "inversePrimary" to MaterialTheme.colorScheme.inversePrimary,
            "secondary" to MaterialTheme.colorScheme.secondary,
            "onSecondary" to MaterialTheme.colorScheme.onSecondary,
            "secondaryContainer" to MaterialTheme.colorScheme.secondaryContainer,
            "onSecondaryContainer" to MaterialTheme.colorScheme.onSecondaryContainer,
            "tertiary" to MaterialTheme.colorScheme.tertiary,
            "onTertiary" to MaterialTheme.colorScheme.onTertiary,
            "tertiaryContainer" to MaterialTheme.colorScheme.tertiaryContainer,
            "onTertiaryContainer" to MaterialTheme.colorScheme.onTertiaryContainer,
            "background" to MaterialTheme.colorScheme.background,
            "onBackground" to MaterialTheme.colorScheme.onBackground,
            "surface" to MaterialTheme.colorScheme.surface,
            "onSurface" to MaterialTheme.colorScheme.onSurface,
            "surfaceVariant" to MaterialTheme.colorScheme.surfaceVariant,
            "onSurfaceVariant" to MaterialTheme.colorScheme.onSurfaceVariant,
            "surfaceTint" to MaterialTheme.colorScheme.surfaceTint,
            "inverseSurface" to MaterialTheme.colorScheme.inverseSurface,
            "inverseOnSurface" to MaterialTheme.colorScheme.inverseOnSurface,
            "error" to MaterialTheme.colorScheme.error,
            "onError" to MaterialTheme.colorScheme.onError,
            "errorContainer" to MaterialTheme.colorScheme.errorContainer,
            "onErrorContainer" to MaterialTheme.colorScheme.onErrorContainer,
            "outline" to MaterialTheme.colorScheme.outline,
            "outlineVariant" to MaterialTheme.colorScheme.outlineVariant,
            "scrim" to MaterialTheme.colorScheme.scrim,
        )
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.developer_mode)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("Sliders", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                SliderShowcase()
                Spacer(Modifier.height(16.dp))
                Text("Progress Indicators", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                ComponentShowcase()
                Spacer(Modifier.height(16.dp))
                Text("Colors", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
            }
            items(colors) { (name, color) ->
                ColorChip(name = name, color = color)
            }
        }
    }
}

@Composable
fun SliderShowcase() {
    val sliderStyles = SliderStyle.values()
    var sliderPosition by remember { mutableStateOf(0.5f) }

    Column {
        sliderStyles.forEach { style ->
            Text(style.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            when (style) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it }
                    )
                }
                SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it }
                    )
                }
                SliderStyle.SLIM -> {
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ComponentShowcase() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
            LinearProgressIndicator()
        }
    }
}

@Composable
fun ColorChip(name: String, color: Color) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(color = color, shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            color = contentColorFor(backgroundColor = color),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
