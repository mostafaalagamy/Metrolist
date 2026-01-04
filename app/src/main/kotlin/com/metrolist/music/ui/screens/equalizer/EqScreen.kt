package com.metrolist.music.ui.screens.equalizer

import android.media.session.PlaybackState
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.eq.data.SavedEQProfile

/**
 * EQ Screen - Manage and select EQ profiles
 */
@Composable
fun EqScreen(
    viewModel: EQViewModel = hiltViewModel(),
    playbackState: PlaybackState? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // File picker for custom EQ import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver

                // Extract file name from URI
                var fileName = "custom_eq.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex >= 0) {
                            val name = cursor.getString(displayNameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    viewModel.importCustomProfile(
                        fileName = fileName,
                        inputStream = inputStream,
                        onSuccess = {
                            showSuccess = true
                        },
                        onError = { error ->
                            showError = error
                        }
                    )
                } else {
                    showError = "Could not read file"
                }
            } catch (e: Exception) {
                showError = "Failed to open file: ${e.message}"
            }
        }
    }

    EqScreenContent(
        profiles = state.profiles,
        activeProfileId = state.activeProfileId,
        onProfileSelected = { viewModel.selectProfile(it) },
        onImportCustomEQ = {
            // Launch file picker for .txt files
            filePickerLauncher.launch("text/plain")
        },
        onDeleteProfile = { viewModel.deleteProfile(it) }
    )

    // Success Snackbar
    if (showSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccess = false
        }
        // Use a Surface for the Snackbar to make it visible above the dialog content if needed,
        // or rely on standard positioning. Since we are in a Dialog, standard Snackbar host might be covered.
        // A simple custom overlay is safer.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text("Custom EQ profile imported successfully")
            }
        }
    }

    // Error dialog
    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Import Error") },
            text = { Text(showError ?: "") },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqScreenContent(
    profiles: List<SavedEQProfile>,
    activeProfileId: String?,
    onProfileSelected: (String?) -> Unit,
    onImportCustomEQ: () -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .padding(vertical = 24.dp) // Optional extra padding if desired, but dialog handles it.
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    val customProfilesCount = profiles.count { it.isCustom }
                    Text(
                        text = "$customProfilesCount ${if (customProfilesCount == 1) "profile" else "profiles"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onImportCustomEQ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import Custom EQ"
                    )
                }
            }

            // Profile list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // "No Equalization" option (always first)
                item {
                    NoEqualizationItem(
                        isSelected = activeProfileId == null,
                        onSelected = { onProfileSelected(null) }
                    )
                }

                // Custom profiles only
                val customProfiles = profiles.filter { it.isCustom }

                if (customProfiles.isNotEmpty()) {
                    items(customProfiles) { profile ->
                        EQProfileItem(
                            profile = profile,
                            isSelected = activeProfileId == profile.id,
                            onSelected = { onProfileSelected(profile.id) },
                            onDelete = { onDeleteProfile(profile.id) }
                        )
                    }
                }

                // Empty state
                if (customProfiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No custom EQ profiles",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onImportCustomEQ) {
                                    Text("Import Profile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
private fun NoEqualizationItem(
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Equalizer Disabled",
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text("Select a profile to apply")
        },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )
        },
        modifier = Modifier
            .clickable(onClick = onSelected)
            .padding(horizontal = 8.dp) // align with design
    )
}

@Composable
private fun EQProfileItem(
    profile: SavedEQProfile,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = profile.deviceModel,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text("${profile.bands.size} bands")
        },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )
        },
        trailingContent = {
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = Modifier
            .clickable(onClick = onSelected)
            .padding(horizontal = 8.dp)
    )

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete \"${profile.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}