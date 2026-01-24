/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.ListenTogetherServerUrlKey
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.listentogether.ListenTogetherEvent
import com.metrolist.music.listentogether.LogEntry
import com.metrolist.music.listentogether.LogLevel
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.PreferenceGroupTitle
import com.metrolist.music.ui.component.SwitchPreference
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ListenTogetherViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ListenTogetherViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    val connectionState by viewModel.connectionState.collectAsState()
    val roomState by viewModel.roomState.collectAsState()
    val role by viewModel.role.collectAsState()
    val pendingJoinRequests by viewModel.pendingJoinRequests.collectAsState()
    val bufferingUsers by viewModel.bufferingUsers.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    var serverUrl by rememberPreference(ListenTogetherServerUrlKey, "ws://metroserver.meowery.eu/ws")
    var username by rememberPreference(ListenTogetherUsernameKey, "")
    
    var showServerUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateRoomDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinRoomDialog by rememberSaveable { mutableStateOf(false) }
    var showLogsDialog by rememberSaveable { mutableStateOf(false) }
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ListenTogetherEvent.RoomCreated -> {
                    Toast.makeText(context, "Room created: ${event.roomCode}", Toast.LENGTH_LONG).show()
                }
                is ListenTogetherEvent.JoinApproved -> {
                    Toast.makeText(context, "Joined room: ${event.roomCode}", Toast.LENGTH_SHORT).show()
                }
                is ListenTogetherEvent.JoinRejected -> {
                    Toast.makeText(context, "Join rejected: ${event.reason}", Toast.LENGTH_SHORT).show()
                }
                is ListenTogetherEvent.JoinRequestReceived -> {
                    Toast.makeText(context, "${event.username} wants to join", Toast.LENGTH_SHORT).show()
                }
                is ListenTogetherEvent.Kicked -> {
                    Toast.makeText(context, "Kicked: ${event.reason}", Toast.LENGTH_SHORT).show()
                }
                is ListenTogetherEvent.ConnectionError -> {
                    Toast.makeText(context, "Connection error: ${event.error}", Toast.LENGTH_SHORT).show()
                }
                is ListenTogetherEvent.ServerError -> {
                    Toast.makeText(context, "Error: ${event.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    // Dialogs
    if (showServerUrlDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.listen_together_server_url)) },
            initialTextFieldValue = TextFieldValue(serverUrl),
            onDone = { serverUrl = it },
            onDismiss = { showServerUrlDialog = false }
        )
    }
    
    if (showUsernameDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.listen_together_username)) },
            initialTextFieldValue = TextFieldValue(username),
            onDone = { username = it },
            onDismiss = { showUsernameDialog = false }
        )
    }
    
    if (showCreateRoomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text(stringResource(R.string.listen_together_create_room)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.listen_together_create_room_desc))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.listen_together_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (username.isNotBlank()) {
                            viewModel.createRoom(username)
                            showCreateRoomDialog = false
                        }
                    },
                    enabled = username.isNotBlank()
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
    
    if (showJoinRoomDialog) {
        AlertDialog(
            onDismissRequest = { showJoinRoomDialog = false },
            title = { Text(stringResource(R.string.listen_together_join_room)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.listen_together_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { roomCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
                        label = { Text(stringResource(R.string.listen_together_room_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (username.isNotBlank() && roomCodeInput.length == 6) {
                            viewModel.joinRoom(roomCodeInput, username)
                            showJoinRoomDialog = false
                            roomCodeInput = ""
                        }
                    },
                    enabled = username.isNotBlank() && roomCodeInput.length == 6
                ) {
                    Text(stringResource(R.string.join))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinRoomDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
    
    if (showLogsDialog) {
        LogsDialog(
            logs = logs,
            onClear = { viewModel.clearLogs() },
            onDismiss = { showLogsDialog = false }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )
        
        // Connection status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionState) {
                    ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                    ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                    ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionState) {
                                    ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFC107)
                                    ConnectionState.ERROR -> Color(0xFFF44336)
                                    ConnectionState.DISCONNECTED -> Color.Gray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                            ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                            ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                            ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                            ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.connect))
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.disconnect))
                        }
                    }
                }
            }
        }
        
        // Room status card (when in a room)
        roomState?.let { state ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.group),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.listen_together_room_code) + ": ${state.roomCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (role == RoomRole.HOST) 
                                    stringResource(R.string.listen_together_you_are_host)
                                else 
                                    stringResource(R.string.listen_together_you_are_guest),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.roomCode))
                                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = {}
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.content_copy),
                                contentDescription = stringResource(R.string.copy)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Users in room
                    Text(
                        text = stringResource(R.string.listen_together_users, state.users.size),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        state.users.forEach { user ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (user.isHost) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (user.isHost) {
                                        Icon(
                                            painter = painterResource(R.drawable.star),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = user.username.ifEmpty { "User" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (user.isHost)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    // Buffering status
                    if (bufferingUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.listen_together_buffering, bufferingUsers.size),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Leave room button
                    OutlinedButton(
                        onClick = { viewModel.leaveRoom() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.logout),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.listen_together_leave_room))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pending join requests (host only)
            if (role == RoomRole.HOST && pendingJoinRequests.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.listen_together_join_requests),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        pendingJoinRequests.forEach { request ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = request.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.approveJoin(request.userId) },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(stringResource(R.string.approve))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                OutlinedButton(
                                    onClick = { viewModel.rejectJoin(request.userId) },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(stringResource(R.string.reject))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Room actions (when connected but not in a room)
        if (connectionState == ConnectionState.CONNECTED && roomState == null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { showCreateRoomDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.listen_together_create_room))
                }
                
                OutlinedButton(
                    onClick = { showJoinRoomDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.group_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.listen_together_join_room))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Settings section
        PreferenceGroupTitle(
            title = stringResource(R.string.settings)
        )
        
        PreferenceEntry(
            title = { Text(stringResource(R.string.listen_together_server_url)) },
            description = serverUrl,
            icon = { Icon(painterResource(R.drawable.cloud), null) },
            onClick = { showServerUrlDialog = true }
        )
        
        PreferenceEntry(
            title = { Text(stringResource(R.string.listen_together_username)) },
            description = username.ifEmpty { stringResource(R.string.not_set) },
            icon = { Icon(painterResource(R.drawable.person), null) },
            onClick = { showUsernameDialog = true }
        )
        
        PreferenceEntry(
            title = { Text(stringResource(R.string.listen_together_view_logs)) },
            description = stringResource(R.string.listen_together_view_logs_desc),
            icon = { Icon(painterResource(R.drawable.bug_report), null) },
            onClick = { showLogsDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.listen_together)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun LogsDialog(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.listen_together_logs)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.listen_together_no_logs),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { log ->
                            LogEntryItem(log)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.clear))
            }
        }
    )
}

@Composable
fun LogEntryItem(log: LogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.timestamp,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (log.level) {
                    LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
                    LogLevel.WARNING -> Color(0xFFFFF3CD)
                    LogLevel.DEBUG -> MaterialTheme.colorScheme.surfaceVariant
                    LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Text(
                    text = log.level.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    color = when (log.level) {
                        LogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        LogLevel.WARNING -> Color(0xFF856404)
                        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                        LogLevel.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        log.details?.let { details ->
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
