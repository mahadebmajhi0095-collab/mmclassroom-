package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Upcoming Live Session"
        val message = intent.getStringExtra("message") ?: "A class is about to start."
        val channelId = intent.getStringExtra("channelId") ?: NotificationHelper.LIVE_CLASS_CHANNEL_ID
        val notificationId = intent.getIntExtra("notificationId", 1001)

        NotificationHelper.postImmediateNotification(
            context = context,
            title = title,
            message = message,
            channelId = channelId,
            notificationId = notificationId
        )
    }
}
