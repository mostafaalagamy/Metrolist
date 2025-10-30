package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.utils.WhitelistSyncProgress

@Composable
fun SplashScreen(
    syncProgress: WhitelistSyncProgress,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App name
            Text(
                text = "Zemer",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading message
            Text(
                text = "Loading artist library...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            if (syncProgress.total > 0) {
                LinearProgressIndicator(
                    progress = { syncProgress.current.toFloat() / syncProgress.total.toFloat() },
                    modifier = Modifier.size(width = 200.dp, height = 4.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress text
                Text(
                    text = "${syncProgress.current} / ${syncProgress.total}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                // Current artist name
                if (syncProgress.currentArtistName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = syncProgress.currentArtistName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Indeterminate progress for initial fetch
                LinearProgressIndicator(
                    modifier = Modifier.size(width = 200.dp, height = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Skip button
            Button(onClick = onSkip) {
                Text(text = "Load in background")
            }
        }
    }
}
