package com.example.foregroundclock

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object  {
        val channelAlarm = "TimerServiceChannel2"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Handle the 5-day timer event here
        // For example, show a notification
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelAlarm)
            .setContentTitle("Tagewecker")
            .setContentText("$name ist dran!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        //can show at most one permission per id, hence once per event. Events themselves can group, this is fine
        notificationManager.notify(eventId, notification)
    }
}
