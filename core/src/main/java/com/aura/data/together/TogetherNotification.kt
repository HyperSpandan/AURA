package com.aura.data.together

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.aura.core.R
import com.aura.data.releases.NewReleases

/**
 * Posts notifications for room chat messages when the user is away from
 * the Listen Together room screen.
 */
object TogetherNotification {
    private const val CHANNEL = "together_chat"
    private const val NOTIFICATION_ID = 0x706574 // "tog"

    /** Tracks whether the user is actively viewing the Together screen */
    @Volatile
    var isViewingRoom: Boolean = false

    fun notifyChat(context: Context, code: RoomCode, senderName: String, text: String, isVoice: Boolean = false) {
        if (isViewingRoom) return
        if (!NewReleases.canNotify(context)) return

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val channel = NotificationChannel(
                CHANNEL,
                "Listen together chat",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Chat messages from your active Listen Together room"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val content = if (isVoice) "🎤 Voice note" else text
        val intent = Intent(Intent.ACTION_VIEW, "aura://together/${code.raw}".toUri())
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            context,
            code.raw.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_aura)
            .setContentTitle(senderName)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked or not granted
        }
    }

    fun clear(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
