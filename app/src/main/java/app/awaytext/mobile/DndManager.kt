package app.awaytext.mobile

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Manager class to handle DND (Do Not Disturb) integration
 */
object DndManager {
    
    private var dndReceiver: DndBroadcastReceiver? = null
    private var isMonitoring = false
    
    /**
     * Start monitoring DND state changes
     */
    fun startMonitoring(context: Context) {
        // Check if notification policy access is granted
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return
        }
        
        // Don't register if already monitoring
        if (isMonitoring) {
            return
        }
        
        // Create and register the receiver using application context to avoid leaks
        dndReceiver = DndBroadcastReceiver()
        val filter = IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        context.applicationContext.registerReceiver(dndReceiver, filter)
        isMonitoring = true
    }
    
    /**
     * Stop monitoring DND state changes
     */
    fun stopMonitoring(context: Context) {
        if (!isMonitoring) {
            return
        }
        
        dndReceiver?.let { receiver ->
            try {
                context.applicationContext.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
            dndReceiver = null
            isMonitoring = false
        }
    }
    
    /**
     * Check if DND is currently active
     */
    fun isDndActive(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check if notification policy access is granted
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return false
        }
        
        val currentFilter = notificationManager.currentInterruptionFilter
        return currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }
    
    /**
     * Manually trigger DND check (useful for when app starts)
     */
    fun checkDndStateAndAutoStart(context: Context) {
        val appPreferences = AppPreferences.getInstance(context)
        
        // Only proceed if auto-start is enabled and app is not already running
        if (!appPreferences.isDndAutoStartEnabled || appPreferences.isAppRunning) {
            return
        }
        
        // Check if DND is active and auto-start AwayText
        if (isDndActive(context)) {
            appPreferences.setAppRunning(true)
            MessageMonitoringService.startService(context)
        }
    }
}
