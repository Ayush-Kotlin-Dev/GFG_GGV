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

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            Log.d("FCM", "Starting notification processing")
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

            // Handle image loading synchronously
            message.data["imageUrl"]?.let { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    try {
                        val connection = URL(imageUrl).openConnection().apply {
                            connectTimeout = 5000
                            readTimeout = 5000
                            doInput = true
                        }
                        connection.connect()

                        connection.getInputStream()?.use { input ->
                            // Load image at a smaller size initially
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 2
                            }
                            val bitmap = BitmapFactory.decodeStream(input, null, options)
                            
                            bitmap?.let { originalBitmap ->
                                // Scale down to exact size needed
                                val finalBitmap = Bitmap.createScaledBitmap(
                                    originalBitmap, 
                                    100, 
                                    100, 
                                    true
                                )
                                notificationBuilder.setLargeIcon(finalBitmap)
                                
                                // Clean up
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

            // Set style after image handling
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message.notification?.body)
            )

            // Show notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())

        } catch (e: Exception) {
            Log.e("FCM", "Error showing notification", e)
            showSimpleNotification(message)
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

}