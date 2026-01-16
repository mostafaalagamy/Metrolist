package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.innertube.models.Artist
import kotlinx.coroutines.delay
import timber.log.Timber


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSelectionDropdownMenu(
    artists: List<Artist>,                  // List of artists from mostPlayedArtists
    selectedArtists: List<Artist>,         // Currently selected artists
    onSelectionChange: (List<Artist>) -> Unit, // Callback for selection changes
    clearArtists: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) } // Tracks if the menu is open
    var searchText by remember { mutableStateOf("") } // Tracks the search text
    var debouncedSearchText by remember { mutableStateOf("") } // Stores the debounced search text

    // Debouncing the search text
    LaunchedEffect(searchText) {
        // Wait for 1 second after the user stops typing
        delay(1000)
        debouncedSearchText = searchText
    }


    // Filter artists based on search text
    val filteredArtists = artists.filter { artist ->
        artist.name.contains(debouncedSearchText, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Search TextField inside ExposedDropdownMenuBox
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }, // Handles expanding/collapsing
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        expanded = true // Expand dropdown while typing
                    },
                    label = { Text("Search and Select Artists") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor() // Attach dropdown to TextField
                )

                // Dropdown Menu
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false } // Close menu when clicking outside
                ) {
                    filteredArtists.forEach { artist ->
                        DropdownMenuItem(
                            onClick = {
                                onSelectionChange(listOf(artist)) // Update selected artist
                                expanded = false // Close the dropdown
                            },
                            text = {
                                Text(text = artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp)) // Spacer between TextField and Clear Button

            // Clear Button on the right edge
            IconButton(
                onClick = {
                    clearArtists() // Clear all selected artists
                    searchText = "" // Clear search input
                    Timber.d("Clear button clicked: All selected artists cleared")
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("╳", fontSize = 24.sp)
            }
        }
    }
}