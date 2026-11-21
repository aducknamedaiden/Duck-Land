package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.PondNotification

class PondNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_TRADES = "duckland_trades"
        const val CHANNEL_EVENTS = "duckland_events"
    }

    private val appContext = context.applicationContext
    private val seenIds = mutableSetOf<String>()
    private var initialSeedCompleted = false

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val tradesChannel = NotificationChannel(
                CHANNEL_TRADES,
                appContext.getString(R.string.channel_trades_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = appContext.getString(R.string.channel_trades_desc)
            }
            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                appContext.getString(R.string.channel_events_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(R.string.channel_events_desc)
            }
            manager.createNotificationChannel(tradesChannel)
            manager.createNotificationChannel(eventsChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun dispatchNewFirestoreNotifications(
        notifications: List<PondNotification>,
        notificationsEnabled: Boolean
    ) {
        if (!initialSeedCompleted) {
            seenIds.addAll(notifications.map { it.id })
            initialSeedCompleted = true
            return
        }
        if (!notificationsEnabled || !hasNotificationPermission()) {
            seenIds.addAll(notifications.map { it.id })
            return
        }

        notifications
            .filter { !it.read && it.id.isNotBlank() && seenIds.add(it.id) }
            .forEach { item ->
                val channel = if (item.type == "event_started" || item.type == "showcase_vote") {
                    CHANNEL_EVENTS
                } else {
                    CHANNEL_TRADES
                }
                showLocalNotification(
                    notificationId = item.id.hashCode(),
                    channelId = channel,
                    title = item.title,
                    body = item.body
                )
            }
    }

    fun showLocalNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission()) return
        createChannels()

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_TRADES) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked mid-session
        }
    }

    fun resetSession() {
        seenIds.clear()
        initialSeedCompleted = false
    }
}
