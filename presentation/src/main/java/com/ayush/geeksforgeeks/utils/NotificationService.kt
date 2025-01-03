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
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    "events_channel",
                    "Events",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Event notifications"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    "updates_channel",
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "App update notifications"
                    enableLights(true)
                    enableVibration(true)
                }
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "APP_UPDATE" -> handleUpdateNotification(message)
            else -> handleEventNotification(message)
        }
    }

    private fun handleUpdateNotification(message: RemoteMessage) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "UPDATE_APP"
                putExtra("downloadUrl", message.data["downloadUrl"])
                putExtra("version", message.data["version"])
                putExtra("releaseNotes", message.data["releaseNotes"])
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )

            val notificationBuilder = NotificationCompat.Builder(this, "updates_channel")
                .setContentTitle(message.notification?.title ?: "Update Available")
                .setContentText(message.notification?.body ?: "A new version is available")
                .setSmallIcon(R.drawable.geeksforgeeks_logo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message.data["releaseNotes"])
                )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(999, notificationBuilder.build())

        } catch (e: Exception) {
            Log.e("FCM", "Error showing update notification", e)
        }
    }

    private fun handleEventNotification(message: RemoteMessage) {
        try {
            Log.d("FCM", "Starting event notification processing")
            Log.d("FCM", "Form Link: ${message.data["formLink"]}")
            Log.d("FCM", "Image URL: ${message.data["imageUrl"]}")

            // Create browser intent
            val formLink = message.data["formLink"]
            val intent = if (!formLink.isNullOrEmpty()) {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(formLink)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
            } else {
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )

            // Build notification
            val notificationBuilder = NotificationCompat.Builder(this, "events_channel")
                .setContentTitle(message.notification?.title)
                .setContentText(message.notification?.body)
                .setSmallIcon(R.drawable.geeksforgeeks_logo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            // Handle image loading
            handleNotificationImage(message.data["imageUrl"], notificationBuilder)

            // Set style after image handling
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message.notification?.body)
            )

            // Show notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())

        } catch (e: Exception) {
            Log.e("FCM", "Error showing event notification", e)
            showSimpleNotification(message)
        }
    }

    private fun handleNotificationImage(imageUrl: String?, builder: NotificationCompat.Builder) {
        imageUrl?.takeIf { it.isNotEmpty() }?.let {
            try {
                val connection = URL(it).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    doInput = true
                }
                connection.connect()

                connection.getInputStream()?.use { input ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    val bitmap = BitmapFactory.decodeStream(input, null, options)
                    
                    bitmap?.let { originalBitmap ->
                        val finalBitmap = Bitmap.createScaledBitmap(
                            originalBitmap, 
                            100, 
                            100, 
                            true
                        )
                        builder.setLargeIcon(finalBitmap)
                        
                        if (finalBitmap != originalBitmap) {
                            originalBitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error loading image", e)
            }
        }
    }

    private fun showSimpleNotification(message: RemoteMessage) {
        val builder = NotificationCompat.Builder(this, "events_channel")
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setSmallIcon(R.drawable.geeksforgeeks_logo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        private const val UPDATE_NOTIFICATION_ID = 999 // Fixed ID for update notifications
    }
}