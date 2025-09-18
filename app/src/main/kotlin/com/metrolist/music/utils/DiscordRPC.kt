package com.metrolist.music.utils

import android.content.Context
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {
    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long, playbackSpeed: Float = 1.0f) = runCatching {
        val currentTime = System.currentTimeMillis()
        val calculatedStartTime = currentTime - currentPlaybackTimeMillis
        
        // Format the title with rate change if different from 1.0
        val appName = context.getString(R.string.app_name).removeSuffix(" Debug")
        val titleWithRate = if (playbackSpeed != 1.0f) {
            "$appName [${String.format("%.1fx", playbackSpeed)}]"
        } else {
            appName
        }
        
        // Calculate adjusted end time based on playback speed
        // If speed is higher, the song will end sooner; if lower, it will take longer
        val remainingDuration = song.song.duration * 1000L - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()
        
        setActivity(
            name = context.getString(R.string.app_name).removeSuffix(" Debug"),
            details = song.song.title,
            state = song.artists.joinToString { it.name },
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            smallImage = song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            largeText = song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = listOf(
                "Listen on YouTube Music" to "https://music.youtube.com/watch?v=${song.song.id}",
                "Visit Metrolist" to "https://github.com/mostafaalagamy/Metrolist"
            ),
            type = Type.LISTENING,
            statusDisplayType = StatusDisplayType.DETAILS,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID
        )
    }

    companion object {
        private const val APPLICATION_ID = "1411019391843172514"
    }
}
