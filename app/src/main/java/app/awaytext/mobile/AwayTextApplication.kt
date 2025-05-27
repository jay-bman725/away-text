package app.awaytext.mobile

import android.app.Application

/**
 * Application class to initialize global components
 */
class AwayTextApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize DND monitoring when the application starts
        val appPreferences = AppPreferences.getInstance(this)
        
        // Only start monitoring if DND auto-start is enabled and notification policy access is granted
        if (appPreferences.isDndAutoStartEnabled) {
            DndManager.startMonitoring(this)
            
            // Check current DND state and auto-start if needed
            DndManager.checkDndStateAndAutoStart(this)
        }
    }
}
