package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.LocalPlayerAwareWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@Composable
fun ReleaseNotesCard() {
    var releaseNotes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        releaseNotes = fetchReleaseNotesText()
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.release_notes),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            releaseNotes.forEach { note ->
                Text(
                    text = "• $note",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

suspend fun fetchReleaseNotesText(): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect("https://github.com/mostafaalagamy/Metrolist/releases/latest").get()
            val changelogElement = document.selectFirst(".markdown-body")
            val htmlContent = changelogElement?.html() ?: "No release notes found"

            val textContent = htmlContent
                .replace(Regex("<br.*?>|</p>"), "\n")
                .replace(Regex("<.*?>"), "")

            textContent.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            listOf("Error loading release notes")
        }
    }
}
