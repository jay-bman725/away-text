package app.awaytext.mobile

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Broadcast receiver that listens for Do Not Disturb state changes
 * and automatically starts/stops AwayText accordingly
 */
class DndBroadcastReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                handleDndStateChange(context)
            }
        }
    }
    
    private fun handleDndStateChange(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val appPreferences = AppPreferences.getInstance(context)
        
        // Check if notification policy access is granted
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return
        }
        
        // Check if DND auto-start feature is enabled
        if (!appPreferences.isDndAutoStartEnabled) {
            return
        }
        
        val currentFilter = notificationManager.currentInterruptionFilter
        val isDndActive = currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        
        // Only auto-start if DND is active and AwayText is not already running
        if (isDndActive && !appPreferences.isAppRunning) {
            // Start AwayText automatically
            appPreferences.setAppRunning(true)
            MessageMonitoringService.startService(context)
            
            // Show a subtle notification to inform the user
            Toast.makeText(
                context, 
                "AwayText started automatically (Do Not Disturb detected)", 
                Toast.LENGTH_SHORT
            ).show()
            
        } else if (!isDndActive && appPreferences.isAppRunning && appPreferences.isDndAutoStopEnabled) {
            // Optionally stop AwayText when DND is turned off (if user enables this feature)
            appPreferences.setAppRunning(false)
            MessageMonitoringService.stopService(context)
            
            Toast.makeText(
                context, 
                "AwayText stopped automatically (Do Not Disturb turned off)", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
