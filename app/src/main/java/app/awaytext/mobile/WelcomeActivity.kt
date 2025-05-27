package app.awaytext.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.awaytext.mobile.ui.theme.AwayTextTheme

class WelcomeActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user has already completed onboarding
        val appPreferences = AppPreferences.getInstance(this)
        if (appPreferences.isOnboardingCompleted) {
            // Skip to MainActivity
            navigateToMainActivity()
            return
        }
        
        enableEdgeToEdge()
        
        setContent {
            AwayTextTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    OnboardingFlow(onCompleted = {
                        // Navigate to MainActivity
                        navigateToMainActivity()
                    })
                }
            }
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish this activity so user can't go back
    }
}
