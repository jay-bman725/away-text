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
}
