package io.github.siddhantpardeshi.aetherplay.space

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.github.siddhantpardeshi.aetherplay.space.bridge.BridgeService

class SpaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    BridgeService.CHANNEL_ID,
                    "Experiment sessions",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows while a game session is available to AetherPlay Lab"
                },
            )
        }
    }
}
