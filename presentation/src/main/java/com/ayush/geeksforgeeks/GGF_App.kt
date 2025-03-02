package com.ayush.geeksforgeeks

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ayush.geeksforgeeks.BuildConfig
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "MyApplication"

@HiltAndroidApp
class MyApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        try {
            // Let Firebase initialize automatically
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
            
            // Initialize and configure Firebase Crashlytics
            initCrashlytics()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }
    }
    
    private fun initCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Disable Crashlytics for debug builds
            crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            Log.d(TAG, "Crashlytics initialized with collection enabled: ${!BuildConfig.DEBUG}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Crashlytics: ${e.message}", e)
        }
    }
    
    companion object {
        /**
         * Log a non-fatal exception to Firebase Crashlytics
         */
        fun logException(throwable: Throwable, message: String? = null) {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                
                message?.let {
                    crashlytics.log("Non-fatal error: $it")
                }
                
                crashlytics.recordException(throwable)
                Log.e(TAG, "Non-fatal error logged to Crashlytics: ${throwable.message}", throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log exception to Crashlytics: ${e.message}", e)
            }
        }
        
        /**
         * Log a custom event to Firebase Crashlytics
         */
        fun logEvent(message: String) {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.log(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log event to Crashlytics: ${e.message}", e)
            }
        }
        
        /**
         * Set a custom key-value pair that will be visible in the Firebase console
         */
        fun setCustomKey(key: String, value: String) {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCustomKey(key, value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set custom key in Crashlytics: ${e.message}", e)
            }
        }
    }
}