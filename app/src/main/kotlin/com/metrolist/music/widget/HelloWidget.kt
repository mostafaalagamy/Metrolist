/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.app.ActivityOptions
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.playback.MusicService

class HelloWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.metrolist.music.ACTION_PLAY"
        const val ACTION_NEXT = "com.metrolist.music.ACTION_NEXT"
        const val ACTION_PREV = "com.metrolist.music.ACTION_PREV"
        const val ACTION_LIKE = "com.metrolist.music.ACTION_LIKE"
        const val ACTION_OPEN_APP = "com.metrolist.music.ACTION_OPEN_APP"

        const val EXTRA_SONG_TITLE = "song_title"
        const val EXTRA_ARTIST_NAME = "artist_name"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_IS_LIKED = "is_liked"
        const val EXTRA_ALBUM_ART = "album_art"

        fun updateWidget(
            context: Context,
            songTitle: String? = null,
            artistName: String? = null,
            isPlaying: Boolean = false,
            isLiked: Boolean = false,
            albumArt: Bitmap? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HelloWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_hello)

                // Update song info
                views.setTextViewText(
                    R.id.txt_song_title,
                    songTitle ?: context.getString(R.string.no_song_playing)
                )
                views.setTextViewText(
                    R.id.txt_artist_name,
                    artistName ?: context.getString(R.string.tap_to_open)
                )

                // Update play/pause button icon
                views.setImageViewResource(
                    R.id.btn_play,
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )

                // Update like button icon
                views.setImageViewResource(
                    R.id.btn_like,
                    if (isLiked) R.drawable.ic_heart else R.drawable.ic_heart_outline
                )

                // Update album art if available
                if (albumArt != null) {
                    views.setImageViewBitmap(R.id.img_album_art, albumArt)
                } else {
                    views.setImageViewResource(R.id.img_album_art, R.mipmap.ic_launcher)
                }

                // Wire up the buttons
                views.setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(context, ACTION_PREV, 1))
                views.setOnClickPendingIntent(R.id.btn_play, getPendingIntent(context, ACTION_PLAY_PAUSE, 2))
                views.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(context, ACTION_NEXT, 3))
                views.setOnClickPendingIntent(R.id.btn_like, getPendingIntent(context, ACTION_LIKE, 4))

                // Wire up the song info area to open app with animation
                views.setOnClickPendingIntent(R.id.widget_song_info, getOpenAppPendingIntent(context))
                views.setOnClickPendingIntent(R.id.img_album_art, getOpenAppPendingIntent(context))

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun getPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, HelloWidget::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun getOpenAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

            val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions.makeBasic().apply {
                    pendingIntentCreatorBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }.toBundle()
            } else {
                null
            }

            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                options
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV, ACTION_LIKE -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    this.action = action
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ACTION_OPEN_APP -> {
                openAppWithAnimation(context)
            }
        }
    }

    private fun openAppWithAnimation(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            options.pendingIntentCreatorBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }

        context.startActivity(intent, options.toBundle())
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_hello)

        // Set default text
        views.setTextViewText(R.id.txt_song_title, context.getString(R.string.no_song_playing))
        views.setTextViewText(R.id.txt_artist_name, context.getString(R.string.tap_to_open))

        // Wire up the buttons
        views.setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(context, ACTION_PREV, 1))
        views.setOnClickPendingIntent(R.id.btn_play, getPendingIntent(context, ACTION_PLAY_PAUSE, 2))
        views.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(context, ACTION_NEXT, 3))
        views.setOnClickPendingIntent(R.id.btn_like, getPendingIntent(context, ACTION_LIKE, 4))

        // Wire up the song info area to open app with animation
        views.setOnClickPendingIntent(R.id.widget_song_info, getOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.img_album_art, getOpenAppPendingIntent(context))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
