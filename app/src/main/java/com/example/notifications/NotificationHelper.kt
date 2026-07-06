package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val LIVE_CLASS_CHANNEL_ID = "live_classes_channel"
    const val QUIZ_CHANNEL_ID = "quiz_assignments_channel"

    /**
     * Initializes notification channels (Required for Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val liveClassChannel = NotificationChannel(
                LIVE_CLASS_CHANNEL_ID,
                "Live Class Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming live classes and interactive sessions"
                enableLights(true)
                enableVibration(true)
            }

            val quizChannel = NotificationChannel(
                QUIZ_CHANNEL_ID,
                "Quiz Assignments",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for newly deployed quiz assignments and test schedules"
                enableLights(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(liveClassChannel)
            manager.createNotificationChannel(quizChannel)
        }
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Only applies to Android 13+ / API 33)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Displays a notification immediately
     */
    fun postImmediateNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String,
        notificationId: Int
    ) {
        // Build an intent to open MainActivity when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback or app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Set custom app icon if possible
        try {
            val appIconResId = context.packageManager.getApplicationInfo(context.packageName, 0).icon
            if (appIconResId != 0) {
                builder.setSmallIcon(appIconResId)
            }
        } catch (e: Exception) {
            // Use fallback
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                notify(notificationId, builder.build())
            }
        }
    }

    /**
     * Schedules a notification for a future timestamp using AlarmManager
     */
    fun scheduleNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String,
        notificationId: Int,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("channelId", channelId)
            putExtra("notificationId", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels a scheduled notification
     */
    fun cancelScheduledNotification(context: Context, notificationId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
