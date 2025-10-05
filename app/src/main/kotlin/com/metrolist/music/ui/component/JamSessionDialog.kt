package com.metrolist.music.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.utils.JamSessionManager

@Composable
fun JamSessionDialog(
    jamSessionManager: JamSessionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentSession by jamSessionManager.currentSession.collectAsState()
    val isHost by jamSessionManager.isHost.collectAsState()
    
    var userName by remember { mutableStateOf("") }
    var sessionCode by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    
    if (currentSession == null) {
        // Not in a session - show create/join options
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = "Spotify Jam",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Listen together with friends!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (showJoinDialog) {
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Your Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = sessionCode,
                            onValueChange = { sessionCode = it.uppercase() },
                            label = { Text("Session Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                if (userName.isNotBlank() && sessionCode.isNotBlank()) {
                                    val success = jamSessionManager.joinSession(sessionCode, userName)
                                    if (success) {
                                        Toast.makeText(context, "Joined session $sessionCode", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Failed to join session", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = userName.isNotBlank() && sessionCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Session")
                        }
                        
                        TextButton(
                            onClick = { showJoinDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back")
                        }
                    } else {
                        Button(
                            onClick = {
                                val code = jamSessionManager.createSession("Host")
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Session Code", code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Session created! Code copied: $code", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Jam Session")
                        }
                        
                        OutlinedButton(
                            onClick = { showJoinDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Jam Session")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        // In a session - show session info
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = "Active Jam Session",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentSession.sessionCode,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                    
                    Text(
                        text = if (isHost) "You are the host" else "Host: ${currentSession.hostName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "${currentSession.participants.size} participant(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Session Code", currentSession.sessionCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied: ${currentSession.sessionCode}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Session Code")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        jamSessionManager.leaveSession()
                        Toast.makeText(context, "Left jam session", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                ) {
                    Text("Leave Session")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}
