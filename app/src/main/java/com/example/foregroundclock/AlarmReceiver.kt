package com.example.foregroundclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val channelAlarm = "TimerServiceChannel2"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.extras?.getString("test")
        val eventId = intent.extras?.getInt("testid") ?: 2
        showNotification(context, name, eventId)
    }

    private fun showNotification(context: Context, name: String?, eventId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Ensure the notification channel is created with proper styling
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, context)

        // Create the notification with Material 3 styling
        val notification = NotificationCompat.Builder(context, channelAlarm)
            .setContentTitle(context.getString(R.string.notification_alarm_title))
            .setContentText(name)
            .setSubText(context.getString(R.string.notification_alarm_subtext))
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Material 3 inspired colors
            .setColor(ContextCompat.getColor(context, android.R.color.system_accent1_600))
            .setColorized(true)
            // Modern notification style
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(context.getString(R.string.notification_alarm_title))
                .bigText(name)
                .setSummaryText(context.getString(R.string.notification_alarm_subtext)))
            .build()

        notificationManager.notify(eventId, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager, context: Context) {
        val channel = NotificationChannel(
            channelAlarm,
            context.getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_alarm_description)
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            setShowBadge(true)
        }
        
        notificationManager.createNotificationChannel(channel)
    }
}
