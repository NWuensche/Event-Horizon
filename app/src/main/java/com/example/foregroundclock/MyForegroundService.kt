package com.example.foregroundclock

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MyForegroundService : Service() {
    private val CHANNEL_ID = "TimerServiceChannel"
    private val NOTIFICATION_ID = 1
    companion object {
        //val ONE_DAY_IN_MILLIS =  60 * 1000L  // 1 day in milliseconds
        val ONE_DAY_IN_MILLIS = 1 * 24 * 60 * 60 * 1000L  // 1 day in milliseconds

    }

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        //setupAlarm()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        intent?.extras?.getString("test2")?.let {
            setupAlarm(it)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        val channelAlarm = NotificationChannel(
            AlarmReceiver.channelAlarm,
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channelAlarm)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Service")
            .setContentText("Timer is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun setupAlarm(name: String) {
//        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val intent = Intent(this, AlarmReceiver::class.java)
//        intent.putExtra("test", name)
//        pendingIntent = PendingIntent.getBroadcast(
//            this,
//            0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Schedule the alarm to repeat every 5 days
//        alarmManager.setRepeating(
//            AlarmManager.RTC_WAKEUP,
//            System.currentTimeMillis() + FIVE_DAYS_IN_MILLIS,
//            FIVE_DAYS_IN_MILLIS,
//            pendingIntent
//        )
    }
}