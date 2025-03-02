package com.ayush.geeksforgeeks.utils

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.MyApplication
import com.ayush.geeksforgeeks.ui.theme.GFGGGVTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Test activity to verify Firebase Crashlytics is working properly.
 * This screen is not integrated into the main app flow and is for testing purposes only.
 * You can use this by uncommenting code in MainActivity to navigate to this screen.
 */
@AndroidEntryPoint
class CrashlyticsTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GFGGGVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashlyticsTestScreen(
                        onNonFatalError = { 
                            logNonFatalError()
                            Toast.makeText(
                                this, 
                                "Non-fatal error logged to Crashlytics", 
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onCustomLog = {
                            logCustomMessage()
                            Toast.makeText(
                                this, 
                                "Custom message logged to Crashlytics", 
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFatalCrash = {
                            // This will crash the app and log to Crashlytics
                            throw RuntimeException("Test crash from CrashlyticsTestActivity")
                        }
                    )
                }
            }
        }
    }
    
    private fun logNonFatalError() {
        try {
            // Simulate an error
            val list = listOf(1, 2, 3)
            list[5] // This will throw IndexOutOfBoundsException
        } catch (e: Exception) {
            // Log the non-fatal exception to Crashlytics
            MyApplication.logException(e, "Accessing invalid index in test activity")
        }
    }
    
    private fun logCustomMessage() {
        // Log a custom event to Crashlytics
        MyApplication.logEvent("User testing Crashlytics from test activity")
        MyApplication.setCustomKey("test_performed", "true")
    }
}

@Composable
fun CrashlyticsTestScreen(
    onNonFatalError: () -> Unit,
    onCustomLog: () -> Unit,
    onFatalCrash: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Firebase Crashlytics Test",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onNonFatalError) {
            Text("Log Non-Fatal Error")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onCustomLog) {
            Text("Log Custom Event")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onFatalCrash) {
            Text("Force App Crash")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Note: Crashlytics events may take up to 24 hours to appear in the Firebase console",
            style = MaterialTheme.typography.bodySmall
        )
    }
}