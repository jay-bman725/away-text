package app.awaytext.mobile

import android.content.Context

/**
 * Helper class to manage application preferences
 */
class AppPreferences private constructor(context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "AwayTextPrefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
        private const val KEY_SMS_PERMISSIONS_GRANTED = "smsPermissionsGranted"
        private const val KEY_CONTACTS_PERMISSION_GRANTED = "contactsPermissionGranted"
        private const val KEY_NOTIFICATION_POLICY_GRANTED = "notificationPolicyGranted"
        private const val KEY_BACKGROUND_PERMISSION_GRANTED = "backgroundPermissionGranted"
        private const val KEY_APP_RUNNING = "appRunning"
        private const val KEY_DND_AUTO_START_ENABLED = "dndAutoStartEnabled"
        private const val KEY_DND_AUTO_STOP_ENABLED = "dndAutoStopEnabled"
        
        @Volatile
        private var instance: AppPreferences? = null
        
        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Check if the onboarding process has been completed
     */
    val isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    
    /**
     * Mark the onboarding process as completed
     */
    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }
    
    /**
     * Reset the onboarding status (for testing purposes)
     */
    fun resetOnboardingStatus() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
    }
    
    // SMS Permissions
    val isSmsPermissionsGranted: Boolean
        get() = prefs.getBoolean(KEY_SMS_PERMISSIONS_GRANTED, false)
    
    fun setSmsPermissionsGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_PERMISSIONS_GRANTED, granted).apply()
    }
    
    // Contacts Permission
    val isContactsPermissionGranted: Boolean
        get() = prefs.getBoolean(KEY_CONTACTS_PERMISSION_GRANTED, false)
    
    fun setContactsPermissionGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_CONTACTS_PERMISSION_GRANTED, granted).apply()
    }
    
    // Notification Policy Permission
    val isNotificationPolicyGranted: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_POLICY_GRANTED, false)
    
    fun setNotificationPolicyGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_POLICY_GRANTED, granted).apply()
    }
    
    // Background Permission
    val isBackgroundPermissionGranted: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_PERMISSION_GRANTED, false)
    
    fun setBackgroundPermissionGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_PERMISSION_GRANTED, granted).apply()
    }
    
    // App Running State
    val isAppRunning: Boolean
        get() = prefs.getBoolean(KEY_APP_RUNNING, false)
    
    fun setAppRunning(running: Boolean) {
        prefs.edit().putBoolean(KEY_APP_RUNNING, running).apply()
    }
    
    // DND Auto Start
    val isDndAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_DND_AUTO_START_ENABLED, false)
    
    fun setDndAutoStartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DND_AUTO_START_ENABLED, enabled).apply()
    }
    
    // DND Auto Stop
    val isDndAutoStopEnabled: Boolean
        get() = prefs.getBoolean(KEY_DND_AUTO_STOP_ENABLED, false)
    
    fun setDndAutoStopEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DND_AUTO_STOP_ENABLED, enabled).apply()
    }
    
    // Reset all permissions (for testing)
    fun resetAllPermissions() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .putBoolean(KEY_SMS_PERMISSIONS_GRANTED, false)
            .putBoolean(KEY_CONTACTS_PERMISSION_GRANTED, false)
            .putBoolean(KEY_NOTIFICATION_POLICY_GRANTED, false)
            .putBoolean(KEY_BACKGROUND_PERMISSION_GRANTED, false)
            .putBoolean(KEY_APP_RUNNING, false)
            .putBoolean(KEY_DND_AUTO_START_ENABLED, false)
            .putBoolean(KEY_DND_AUTO_STOP_ENABLED, false)
            .apply()
    }
}
