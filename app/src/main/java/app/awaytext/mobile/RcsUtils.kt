package app.awaytext.mobile

import android.content.Context
import android.util.Log

/**
 * Utility class for RCS capability detection and management
 */
object RcsUtils {
    
    private const val TAG = "RcsUtils"
    
    /**
     * Check if RCS is supported and enabled on this device
     */
    fun isRcsSupported(context: Context): Boolean {
        return try {
            val rcsHandler = RcsMessageHandler(context)
            rcsHandler.isRcsAvailable()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking RCS support: ${e.message}")
            false
        }
    }
    
    /**
     * Get a user-friendly status message about RCS capabilities
     */
    fun getRcsStatusMessage(context: Context): String {
        return if (isRcsSupported(context)) {
            "RCS messaging is available and enabled. Your auto-replies will use RCS when possible, with SMS as fallback."
        } else {
            "RCS messaging is not available on this device. Auto-replies will use SMS."
        }
    }
    
    /**
     * Get a short RCS status for display in UI
     */
    fun getRcsShortStatus(context: Context): String {
        return if (isRcsSupported(context)) {
            "SMS/RCS Ready"
        } else {
            "SMS Ready"
        }
    }
    
    /**
     * Check if we have the necessary permissions for RCS
     */
    fun hasRcsPermissions(context: Context): Boolean {
        // Since RCS permissions are not widely standardized yet,
        // we check for basic messaging permissions
        return AppPreferences.getInstance(context).isSmsPermissionsGranted
    }
    
    /**
     * Format milliseconds into a human-readable duration string
     * @param milliseconds The duration in milliseconds
     * @return Human-readable string (e.g. "3 mins 45 secs")
     */
    fun formatDuration(milliseconds: Long): String {
        if (milliseconds <= 0) return "0 secs"
        
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        
        return when {
            minutes > 0 -> "$minutes ${if (minutes == 1L) "min" else "mins"} $seconds ${if (seconds == 1L) "sec" else "secs"}"
            else -> "$seconds ${if (seconds == 1L) "sec" else "secs"}"
        }
    }
    
    /**
     * Get cooldown status for a specific sender
     * @param context Application context
     * @param sender The sender's phone number
     * @return Human-readable cooldown status
     */
    fun getCooldownStatus(context: Context, sender: String): String {
        val appPreferences = AppPreferences.getInstance(context)
        val remainingTime = appPreferences.getRemainingCooldownTime(sender)
        
        return if (remainingTime <= 0) {
            "Ready to send"
        } else {
            "Cooldown: ${formatDuration(remainingTime)} remaining"
        }
    }
}
