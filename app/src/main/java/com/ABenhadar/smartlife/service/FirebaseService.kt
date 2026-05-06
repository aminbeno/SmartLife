package com.ABenhadar.smartlife.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ABenhadar.smartlife.MainActivity
import com.ABenhadar.smartlife.R
import com.ABenhadar.smartlife.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FirebaseService : FirebaseMessagingService() {

    private val repository = UserRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_Token", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_Message", "From: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d("FCM_Message", "Notification Message Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        val userId = auth.currentUser?.uid ?: return
        if (token == null) return

        serviceScope.launch {
            repository.updateUserFCMToken(userId, token)
                .onSuccess { Log.d("FCM_Token", "FCM token updated successfully on backend") }
                .onFailure { Log.e("FCM_Token", "Failed to update FCM token on backend: ${it.message}") }
        }
    }

    private fun sendNotification(title: String?, body: String?) {
        val channelId = "smartlife_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .setContentTitle(title ?: "SmartLife Notification")
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        notificationBuilder.setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "SmartLife Notifications",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
