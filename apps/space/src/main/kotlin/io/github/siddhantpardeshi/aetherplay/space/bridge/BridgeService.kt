package io.github.siddhantpardeshi.aetherplay.space.bridge

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.siddhantpardeshi.aetherplay.protocol.BridgeContract
import io.github.siddhantpardeshi.aetherplay.space.MainActivity
import io.github.siddhantpardeshi.aetherplay.space.R
import io.github.siddhantpardeshi.aetherplay.space.game.GameActivity

/**
 * Hosts the active experiment session: keeps the process alive while a game
 * session runs, exposes the signature-guarded broker to AetherPlay Lab, and
 * shows an ongoing notification with a stop action.
 */
class BridgeService : Service() {

    companion object {
        const val ACTION_START = "io.github.siddhantpardeshi.aetherplay.space.session.START"
        const val ACTION_STOP = "io.github.siddhantpardeshi.aetherplay.space.session.STOP"
        const val EXTRA_GAME = "game"
        const val EXTRA_SPACE = "space"
        const val CHANNEL_ID = "aether_sessions"
        const val NOTIFICATION_ID = 4201

        fun startSession(context: Context, gameKey: String, spaceKey: String) {
            val intent = Intent(context, BridgeService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_GAME, gameKey)
                putExtra(EXTRA_SPACE, spaceKey)
            }
            context.startForegroundService(intent)
        }

        fun stopSession(context: Context) {
            context.startService(Intent(context, BridgeService::class.java).apply { action = ACTION_STOP })
        }
    }

    private lateinit var manager: SessionManager

    override fun onCreate() {
        super.onCreate()
        manager = SessionManager(this)
    }

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == BridgeContract.ACTION) manager.broker.asBinder() else null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val game = intent.getStringExtra(EXTRA_GAME) ?: return invalidStart()
                val space = intent.getStringExtra(EXTRA_SPACE) ?: return invalidStart()
                val result = manager.startSessionFromUi(game, space)
                if (result.code == 0) {
                    startInForeground()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                manager.stopSession(save = true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Restarted with no action: show the notification again if a session survived.
                if (SessionBus.state.value.state == io.github.siddhantpardeshi.aetherplay.protocol.SessionState.RUNNING) {
                    startInForeground()
                } else {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun invalidStart(): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openGame = PendingIntent.getActivity(
            this, 1, Intent(this, GameActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, BridgeService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aether)
            .setContentTitle("AetherPlay session active")
            .setContentText("Crystal Sprint is available to AetherPlay Lab")
            .setContentIntent(open)
            .addAction(0, "Open game", openGame)
            .addAction(0, "Stop session", stop)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        manager.shutdown()
        super.onDestroy()
    }
}
