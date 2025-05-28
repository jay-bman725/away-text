package app.awaytext.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

/**
 * Unified receiver for handling both SMS and RCS messages
 */
class UnifiedMessageReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "UnifiedMessageReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                handleSmsReceived(context, intent)
            }
            Telephony.Mms.Intents.CONTENT_CHANGED_ACTION -> {
                handleMmsReceived(context, intent)
            }
            // RCS messages are typically handled through content observers
            // since there's no standard broadcast for RCS
        }
    }
    
    private fun handleSmsReceived(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>?
            if (pdus != null) {
                for (pdu in pdus) {
                    val format = bundle.getString("format")
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray, format)
                    val messageBody = smsMessage.messageBody
                    val sender = smsMessage.displayOriginatingAddress
                    
                    Log.d(TAG, "SMS received from: $sender")
                    
                    // Notify the message monitoring service
                    val serviceIntent = Intent(context, MessageMonitoringService::class.java).apply {
                        putExtra("message_type", "SMS")
                        putExtra("sender", sender)
                        putExtra("body", messageBody)
                        putExtra("timestamp", System.currentTimeMillis())
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
    
    private fun handleMmsReceived(context: Context, intent: Intent) {
        Log.d(TAG, "MMS content changed, checking for new messages")
        
        // For MMS/RCS, we need to query the content provider
        val serviceIntent = Intent(context, MessageMonitoringService::class.java).apply {
            putExtra("check_mms_rcs", true)
        }
        context.startService(serviceIntent)
    }
}
