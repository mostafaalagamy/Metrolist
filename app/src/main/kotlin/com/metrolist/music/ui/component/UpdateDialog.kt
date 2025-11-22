package com.metrolist.music.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.metrolist.music.R
import kotlinx.coroutines.delay

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onBackup: () -> Unit
) {
    var updateButtonEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3000)
        updateButtonEnabled = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_dialog_title)) },
        text = { Text(text = stringResource(R.string.update_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onUpdate()
                    onDismiss()
                },
                enabled = updateButtonEnabled
            ) {
                Text(text = stringResource(R.string.update_anyways))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onBackup()
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.create_backup))
            }
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}
