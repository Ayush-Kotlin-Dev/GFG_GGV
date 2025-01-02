package com.ayush.geeksforgeeks.utils

import com.ayush.geeksforgeeks.MainActivity
import com.ayush.geeksforgeeks.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL

class NotificationService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Update token in Firestore
        updateTokenInFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Debug logging
        Log.d("FCM", "Message data: ${message.data}")
        Log.d("FCM", "Form link: ${message.data["formLink"]}")
        Log.d("FCM", "Image URL: ${message.data["imageUrl"]}")

        // Create intent with flags for browser opening
        val intent = when {
            !message.data["formLink"].isNullOrEmpty() -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(message.data["formLink"])
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    // Add this flag to prefer external browser
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }
            else -> Intent(this, MainActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification with big picture style if image exists
        val notificationBuilder = NotificationCompat.Builder(this, "events_channel")
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.geeksforgeeks_logo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body))

        // Add image as large icon if exists
        if (!message.data["imageUrl"].isNullOrEmpty()) {
            try {
                val bitmap = getBitmapFromUrl(message.data["imageUrl"]!!)
                bitmap?.let {
                    notificationBuilder.setLargeIcon(it)
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error loading image", e)
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: Exception) {
            Log.e("FCM", "Error loading image: ${e.message}")
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = maxSize.toFloat() / Math.max(width, height)

        return if (ratio < 1) {
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "events_channel",
                "Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Event notifications"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateTokenInFirestore(token: String) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "tokenUpdatedAt" to FieldValue.serverTimestamp(),
                        "isLoggedIn" to true // Also update login status
                    )
                )
                .addOnFailureListener { e ->
                    Log.e("FCM", "Failed to update token", e)
                }
        }
    }
}