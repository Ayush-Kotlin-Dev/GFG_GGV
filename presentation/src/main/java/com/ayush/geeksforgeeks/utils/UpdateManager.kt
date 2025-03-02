package com.ayush.geeksforgeeks.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class UpdateManager(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadAndInstallUpdate(downloadUrl: String) {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("App Update")
            .setDescription("Downloading latest version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GFG_${android.text.format.DateFormat.format("dd MMM yyyy", System.currentTimeMillis())}_app-update.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)

        // Add download progress tracking
        val progressChecker = object : Runnable {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )
                    
                    val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                    android.util.Log.d("UpdateManager", "Download progress: $progress%")
                    
                    if (bytesDownloaded != bytesTotal) {
                        // Check again in 1 second
                        android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(this, 1000)
                    }
                }
                cursor.close()
            }
        }
        progressChecker.run()

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (columnIndex != -1 && cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            installApk(downloadId)
                        }
                    }
                    cursor.close()
                    context.unregisterReceiver(this)
                }
            }
        }

        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(downloadId: Long) {
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (columnIndex != -1) {
                val uriString = cursor.getString(columnIndex)
                if (uriString != null) {
                    try {
                        val uri = Uri.parse(uriString)
                        // Get the file path safely
                        val file = getFileFromUri(uri)
                        if (file != null && file.exists()) {
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )

                            val install = Intent(Intent.ACTION_VIEW).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                setDataAndType(contentUri, "application/vnd.android.package-archive")
                            }
                            context.startActivity(install)
                        } else {
                            android.util.Log.e("UpdateManager", "Downloaded file does not exist or is null")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UpdateManager", "Error installing APK: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
        cursor.close()
    }
    
    // Helper method to safely get File from Uri
    private fun getFileFromUri(uri: Uri): File? {
        // Handle content URIs
        if (uri.scheme == "content") {
            // Try to get the path from the content URI
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                    if (columnIndex != -1) {
                        val filePath = it.getString(columnIndex)
                        if (!filePath.isNullOrEmpty()) {
                            return File(filePath)
                        }
                    }
                }
            }
            
            // If we couldn't get the file path, try a different approach for downloads
            if (uri.toString().startsWith("content://downloads")) {
                val segments = uri.pathSegments
                if (segments != null && segments.size > 1) {
                    val id = segments[segments.size - 1]
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    // Try to find the file in the downloads directory
                    downloads?.listFiles()?.forEach {
                        if (it.name.contains("GFG_") && it.name.endsWith("_app-update.apk")) {
                            return it
                        }
                    }
                }
            }
        } 
        
        // For file URIs
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                return File(path)
            }
        }
        
        // Fallback to direct download directory
        val fileName = "GFG_${android.text.format.DateFormat.format("dd MMM yyyy", System.currentTimeMillis())}_app-update.apk"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, fileName)
    }
}