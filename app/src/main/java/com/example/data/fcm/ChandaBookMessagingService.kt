package com.example.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChandaBookMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New registration token received: $token")
        // In real app, send token to user/auth session backend if authenticated
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // 1. Extract notification title and body
        var title = remoteMessage.notification?.title
        var body = remoteMessage.notification?.body
        var targetScreen = remoteMessage.data["target_screen"] ?: ""

        // If data payload exists, use its title and body as overrides/primary
        if (remoteMessage.data.isNotEmpty()) {
            title = remoteMessage.data["title"] ?: title ?: "ChandaBook LEDGER"
            body = remoteMessage.data["body"] ?: body ?: "Important updates received."
        }

        if (title != null && body != null) {
            sendNotification(title, body, targetScreen)
        }
    }

    private fun sendNotification(title: String, body: String, targetScreen: String) {
        val channelId = "chandabook_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ChandaBook Alerts & Ledger Payouts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Chanda totals, upcoming bills, and announcements"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Set up tap intent to launch MainActivity and route to target section
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FCM_TARGET_SCREEN", targetScreen)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Draw system notification matching the branded TealDark accent color
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System standard fallback
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setColor(0xFF0F2F3A.toInt()) // Brand TealDark Hex (0xFF0F2F3A)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
