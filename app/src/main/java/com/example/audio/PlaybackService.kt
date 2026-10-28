package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.R
import com.example.model.Track

class PlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "aura_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_UPDATE_STATE = "com.example.action.UPDATE_STATE"

        var currentPlayingTrack: Track? = null
        var isCurrentlyPlaying: Boolean = false

        var onPlayPauseAction: (() -> Unit)? = null
        var onStopAction: (() -> Unit)? = null
        var onNextAction: (() -> Unit)? = null
        var onPrevAction: (() -> Unit)? = null

        fun updateState(context: Context, track: Track?, isPlaying: Boolean) {
            currentPlayingTrack = track
            isCurrentlyPlaying = isPlaying

            if (track == null) {
                stopPlaybackService(context)
                return
            }

            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_UPDATE_STATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // If app is in background and cannot start foreground service
            }
        }

        fun stopPlaybackService(context: Context) {
            try {
                val intent = Intent(context, PlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        // Notification actions must not crash the foreground service if a stale callback fails.
        try {
            when (action) {
                ACTION_PLAY_PAUSE -> onPlayPauseAction?.invoke()
                ACTION_STOP -> {
                    onStopAction?.invoke()
                    stopForegroundNotification()
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_NEXT -> onNextAction?.invoke()
                ACTION_PREV -> onPrevAction?.invoke()
            }
        } catch (_: Exception) {
            // Keep playback service alive if a player callback throws.
        }

        val track = currentPlayingTrack
        if (track != null) {
            val notification = buildMediaNotification(track, isCurrentlyPlaying)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                // Handle ForegroundServiceStartNotAllowedException on Android 12+ if in background
                try {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {
                    // Some Android builds restrict fallback notification updates.
                }
            }
        } else {
            stopForegroundNotification()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun stopForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Sound Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol jendela pemutar musik di latar belakang"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildMediaNotification(track: Track, isPlaying: Boolean): Notification {
        // Content click intent -> opens MainActivity
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(
            this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause Action
        val playPauseIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePending = PendingIntent.getService(
            this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next Action
        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(
            this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action
        val stopIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play
        val playPauseTitle = if (isPlaying) "Jeda" else "Putar"

        val coverBitmap = com.example.data.AudioCoverArtHelper.loadTrackCoverBitmap(this, track)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_play)
            .setLargeIcon(coverBitmap)
            .setColor(0xFF000000.toInt())
            .setColorized(true)
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.album}")
            .setSubText(track.genre)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .addAction(R.drawable.ic_notification_prev, "Sebelumnya", prevPending) // action 0
            .addAction(playPauseIcon, playPauseTitle, playPausePending)             // action 1
            .addAction(R.drawable.ic_notification_next, "Berikutnya", nextPending) // action 2
            .addAction(R.drawable.ic_notification_stop, "Berhenti", stopPending)   // action 3
            .setStyle(
                MediaStyle()
                    // Show Previous, Play/Pause, and Stop in compact view (indices 0, 1, 3)
                    .setShowActionsInCompactView(0, 1, 3)
            )

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundNotification()
    }
}
