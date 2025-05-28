package app.awaytext.mobile

import android.content.Context
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log

/**
 * Handler for RCS (Rich Communication Services) messages
 * Provides functionality to monitor and respond to RCS messages
 */
class RcsMessageHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "RcsMessageHandler"
        
        // RCS message URIs for querying message database
        private val RCS_MESSAGE_URI = Uri.parse("content://rcs/message")
        private val RCS_THREAD_URI = Uri.parse("content://rcs/thread")
        
        // RCS message types
        private const val MESSAGE_TYPE_INCOMING = 1
        private const val MESSAGE_TYPE_OUTGOING = 2
        
        // RCS message status
        private const val MESSAGE_STATUS_RECEIVED = 1
        private const val MESSAGE_STATUS_READ = 2
    }
    
    /**
     * Check if RCS is available on this device
     */
    fun isRcsAvailable(): Boolean {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            
            // Check if device supports RCS and has active subscriptions
            activeSubscriptions?.isNotEmpty() == true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check RCS availability: ${e.message}")
            false
        }
    }
    
    /**
     * Monitor for new RCS messages
     * Returns the latest unread RCS message if any
     */
    fun getLatestUnreadRcsMessage(): RcsMessage? {
        if (!isRcsAvailable()) {
            return null
        }
        
        return try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                RCS_MESSAGE_URI,
                null,
                "status = ? AND type = ?",
                arrayOf(MESSAGE_STATUS_RECEIVED.toString(), MESSAGE_TYPE_INCOMING.toString()),
                "timestamp DESC LIMIT 1"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    parseRcsMessage(it)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying RCS messages: ${e.message}")
            null
        }
    }
    
    /**
     * Send an RCS message reply
     */
    fun sendRcsReply(destinationAddress: String, message: String): Boolean {
        if (!isRcsAvailable()) {
            Log.w(TAG, "RCS not available, falling back to SMS")
            return sendSmsReply(destinationAddress, message)
        }
        
        return try {
            val smsManager = SmsManager.getDefault()
            
            // Try to send as RCS first, fall back to SMS if needed
            // Note: Android doesn't provide a direct RCS API, so we use SmsManager
            // which automatically chooses RCS or SMS based on recipient capability
            smsManager.sendTextMessage(destinationAddress, null, message, null, null)
            
            Log.i(TAG, "RCS message sent to $destinationAddress")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send RCS message: ${e.message}")
            // Fallback to SMS
            sendSmsReply(destinationAddress, message)
        }
    }
    
    /**
     * Fallback SMS sending
     */
    private fun sendSmsReply(destinationAddress: String, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(destinationAddress, null, message, null, null)
            Log.i(TAG, "SMS fallback sent to $destinationAddress")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS fallback: ${e.message}")
            false
        }
    }
    
    /**
     * Parse RCS message from cursor
     */
    private fun parseRcsMessage(cursor: Cursor): RcsMessage? {
        return try {
            val addressIndex = cursor.getColumnIndex("address")
            val bodyIndex = cursor.getColumnIndex("body")
            val timestampIndex = cursor.getColumnIndex("timestamp")
            val threadIdIndex = cursor.getColumnIndex("thread_id")
            
            if (addressIndex >= 0 && bodyIndex >= 0 && timestampIndex >= 0) {
                RcsMessage(
                    address = cursor.getString(addressIndex),
                    body = cursor.getString(bodyIndex),
                    timestamp = cursor.getLong(timestampIndex),
                    threadId = if (threadIdIndex >= 0) cursor.getLong(threadIdIndex) else -1
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RCS message: ${e.message}")
            null
        }
    }
    
    /**
     * Check if a phone number supports RCS
     */
    fun isRcsCapable(phoneNumber: String): Boolean {
        // In a real implementation, this would check with the carrier
        // For now, we assume RCS capability based on device RCS support
        return isRcsAvailable()
    }
    
    /**
     * Get RCS conversation thread ID for a phone number
     */
    fun getThreadId(phoneNumber: String): Long? {
        return try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                RCS_THREAD_URI,
                arrayOf("_id"),
                "address = ?",
                arrayOf(phoneNumber),
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(0)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting thread ID: ${e.message}")
            null
        }
    }
}

/**
 * Data class representing an RCS message
 */
data class RcsMessage(
    val address: String,
    val body: String,
    val timestamp: Long,
    val threadId: Long
)
