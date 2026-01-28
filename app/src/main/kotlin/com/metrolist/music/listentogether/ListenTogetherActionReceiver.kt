package com.metrolist.music.listentogether

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListenTogetherActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val client = ListenTogetherClient.getInstance() ?: return
        val notifId = intent.getIntExtra(ListenTogetherClient.EXTRA_NOTIFICATION_ID, 0)

        // Cancel the notification immediately
        NotificationManagerCompat.from(context).cancel(notifId)

        when (intent.action) {
            ListenTogetherClient.ACTION_APPROVE_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    client.approveJoin(userId)
                }
            }
            ListenTogetherClient.ACTION_REJECT_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    client.rejectJoin(userId, null)
                }
            }
            ListenTogetherClient.ACTION_APPROVE_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    client.approveSuggestion(suggestionId)
                }
            }
            ListenTogetherClient.ACTION_REJECT_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    client.rejectSuggestion(suggestionId, null)
                }
            }
        }
    }
}
