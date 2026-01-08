package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.Artist
import timber.log.Timber


@Composable
fun ArtistSelectionDropdownMenu(
    artists: List<Artist>,                  // List of artists from mostPlayedArtists
    selectedArtists: List<Artist>,         // Currently selected artists
    onSelectionChange: (List<Artist>) -> Unit, // Callback for selection changes
    clearArtists: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) } // Tracks if the menu is open
    var searchText by remember { mutableStateOf("") } // Tracks the search text

    // Filter artists based on search text
    val filteredArtists = artists.filter { artist ->
        artist.name.contains(searchText, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Main TextField that triggers the dropdown
            TextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    expanded = true // Open menu when typing
                },
                label = { Text("Search and Select Artists") },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        // You can customize the trailing icon here if needed
                    }
                },
                modifier = Modifier.weight(1f) // Let the TextField take up available space
            )

            Spacer(modifier = Modifier.width(8.dp)) // Add spacing between TextField and button

            // Clear button
            IconButton(
                onClick = {
                    clearArtists() // Clear all selected artists
                    Timber.d("Clear button clicked: All selected artists cleared")
                }
            ) {
                Text("Clear") // Replace with an icon if desired, e.g., Icons.Default.Close
            }
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Items in the dropdown
            filteredArtists.forEach { artist ->
                DropdownMenuItem(
                    onClick = {
                        onSelectionChange(listOf(artist))
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Checkbox
                            Checkbox(
                                checked = selectedArtists.contains(artist),
                                onCheckedChange = null // No local state here
                            )

                            // Artist name
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                )
            }
        }
    }
}