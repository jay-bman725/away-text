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
        
        // Message settings
        private const val KEY_DEFAULT_AWAY_MESSAGE = "defaultAwayMessage"
        private const val KEY_CUSTOM_CONTACT_MESSAGES = "customContactMessages"
        private const val KEY_USE_CUSTOM_CONTACT_MESSAGES = "useCustomContactMessages"
        
        // Version checking
        private const val KEY_LAST_VERSION_CHECK = "lastVersionCheck"
        private const val KEY_LATEST_KNOWN_VERSION = "latestKnownVersion"
        private const val KEY_VERSION_UPDATE_DISMISSED = "versionUpdateDismissed"
        
        // Last reply timestamps prefix
        private const val KEY_LAST_REPLY_PREFIX = "lastReplyTimestamp_"
        
        // Reply cooldown period (5 minutes in milliseconds)
        private const val REPLY_COOLDOWN_PERIOD_MS = 5 * 60 * 1000L
        
        // Default away message
        private const val DEFAULT_AWAY_MESSAGE = "I'm currently away and will respond to your message later. This is an automated reply from AwayText."
        
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
    
    // Default Away Message
    var defaultAwayMessage: String
        get() = prefs.getString(KEY_DEFAULT_AWAY_MESSAGE, DEFAULT_AWAY_MESSAGE) ?: DEFAULT_AWAY_MESSAGE
        set(message) = prefs.edit().putString(KEY_DEFAULT_AWAY_MESSAGE, message).apply()
    
    // Custom Contact Messages
    var customContactMessages: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_CONTACT_MESSAGES, emptySet()) ?: emptySet()
        set(messages) = prefs.edit().putStringSet(KEY_CUSTOM_CONTACT_MESSAGES, messages).apply()
    
    // Use Custom Contact Messages
    var useCustomContactMessages: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_CONTACT_MESSAGES, false)
        set(enabled) = prefs.edit().putBoolean(KEY_USE_CUSTOM_CONTACT_MESSAGES, enabled).apply()
    
    // Last Version Check
    var lastVersionCheck: Long
        get() = prefs.getLong(KEY_LAST_VERSION_CHECK, 0L)
        set(timestamp) = prefs.edit().putLong(KEY_LAST_VERSION_CHECK, timestamp).apply()
    
    // Latest Known Version
    var latestKnownVersion: String
        get() = prefs.getString(KEY_LATEST_KNOWN_VERSION, "") ?: ""
        set(version) = prefs.edit().putString(KEY_LATEST_KNOWN_VERSION, version).apply()
    
    // Version Update Dismissed
    var isVersionUpdateDismissed: Boolean
        get() = prefs.getBoolean(KEY_VERSION_UPDATE_DISMISSED, false)
        set(dismissed) = prefs.edit().putBoolean(KEY_VERSION_UPDATE_DISMISSED, dismissed).apply()
    
    // Helper methods for custom contact messages
    fun addCustomContactMessage(phoneNumber: String, message: String) {
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        val currentMessages = customContactMessages.toMutableSet()
        // Remove any existing entry for this number (in case of different formatting)
        currentMessages.removeAll { entry ->
            val storedNumber = entry.substringBefore("|")
            normalizePhoneNumber(storedNumber) == normalizedNumber
        }
        currentMessages.add("$phoneNumber|$message")
        customContactMessages = currentMessages
    }
    
    fun removeCustomContactMessage(phoneNumber: String) {
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        val currentMessages = customContactMessages.toMutableSet()
        currentMessages.removeAll { entry ->
            val storedNumber = entry.substringBefore("|")
            normalizePhoneNumber(storedNumber) == normalizedNumber
        }
        customContactMessages = currentMessages
    }
    
    fun getCustomMessageForContact(phoneNumber: String): String? {
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        return customContactMessages.find { entry ->
            val storedNumber = entry.substringBefore("|")
            val normalizedStoredNumber = normalizePhoneNumber(storedNumber)
            normalizedStoredNumber == normalizedNumber ||
            normalizedStoredNumber.endsWith(normalizedNumber.takeLast(10)) ||
            normalizedNumber.endsWith(normalizedStoredNumber.takeLast(10))
        }?.substringAfter("|")
    }
    
    fun getAllCustomContactMessages(): Map<String, String> {
        return customContactMessages.associate { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                entry to ""
            }
        }
    }
    
    // Message timestamp tracking
    fun setLastReplyTimestampForContact(phoneNumber: String, timestamp: Long) {
        prefs.edit().putLong("$KEY_LAST_REPLY_PREFIX$phoneNumber", timestamp).apply()
    }
    
    fun getLastReplyTimestampForContact(phoneNumber: String): Long {
        return prefs.getLong("$KEY_LAST_REPLY_PREFIX$phoneNumber", 0L)
    }
    
    fun hasReplyCooldownPassed(phoneNumber: String): Boolean {
        val lastReplyTimestamp = getLastReplyTimestampForContact(phoneNumber)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastReplyTimestamp) >= REPLY_COOLDOWN_PERIOD_MS
    }
    
    // Reset all permissions and settings (for testing)
    fun resetAllPermissionsAndSettings() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .putBoolean(KEY_SMS_PERMISSIONS_GRANTED, false)
            .putBoolean(KEY_CONTACTS_PERMISSION_GRANTED, false)
            .putBoolean(KEY_NOTIFICATION_POLICY_GRANTED, false)
            .putBoolean(KEY_BACKGROUND_PERMISSION_GRANTED, false)
            .putBoolean(KEY_APP_RUNNING, false)
            .putBoolean(KEY_DND_AUTO_START_ENABLED, false)
            .putBoolean(KEY_DND_AUTO_STOP_ENABLED, false)
            .putString(KEY_DEFAULT_AWAY_MESSAGE, DEFAULT_AWAY_MESSAGE)
            .putStringSet(KEY_CUSTOM_CONTACT_MESSAGES, emptySet())
            .putBoolean(KEY_USE_CUSTOM_CONTACT_MESSAGES, false)
            .putLong(KEY_LAST_VERSION_CHECK, 0L)
            .putString(KEY_LATEST_KNOWN_VERSION, "")
            .putBoolean(KEY_VERSION_UPDATE_DISMISSED, false)
            .apply()
    }
    
    /**
     * Record the timestamp when a reply was last sent to a specific sender
     * @param sender The phone number of the message sender
     */
    fun recordLastReplyTimestamp(sender: String) {
        val normalizedSender = normalizePhoneNumber(sender)
        prefs.edit().putLong("${KEY_LAST_REPLY_PREFIX}$normalizedSender", System.currentTimeMillis()).apply()
    }
    
    /**
     * Check if it's okay to send another reply to this sender based on the cooldown period
     * @param sender The phone number of the message sender
     * @return true if it's been longer than the cooldown period since the last reply, false otherwise
     */
    fun canSendReplyToSender(sender: String): Boolean {
        val normalizedSender = normalizePhoneNumber(sender)
        val lastReplyTime = prefs.getLong("${KEY_LAST_REPLY_PREFIX}$normalizedSender", 0L)
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - lastReplyTime) > REPLY_COOLDOWN_PERIOD_MS
    }
    
    /**
     * Get the remaining cooldown time in milliseconds before another reply can be sent to this sender
     * @param sender The phone number of the message sender
     * @return Remaining time in milliseconds, or 0 if cooldown period has elapsed
     */
    fun getRemainingCooldownTime(sender: String): Long {
        val normalizedSender = normalizePhoneNumber(sender)
        val lastReplyTime = prefs.getLong("${KEY_LAST_REPLY_PREFIX}$normalizedSender", 0L)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastReplyTime
        
        return if (elapsed >= REPLY_COOLDOWN_PERIOD_MS) {
            0L
        } else {
            REPLY_COOLDOWN_PERIOD_MS - elapsed
        }
    }

    /**
     * Clear all the stored reply timestamps (mainly for testing)
     */
    fun clearAllReplyTimestamps() {
        val allPrefs = prefs.all
        val editor = prefs.edit()
        
        allPrefs.keys.filter { it.startsWith(KEY_LAST_REPLY_PREFIX) }
            .forEach { key -> editor.remove(key) }
            
        editor.apply()
    }
    
    /**
     * Normalize phone numbers to ensure consistent lookup regardless of format
     * Strips all non-digit characters
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }
}
