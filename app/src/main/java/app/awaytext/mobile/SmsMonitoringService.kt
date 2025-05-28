package app.awaytext.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class SmsMonitoringService : Service() {
    
    private lateinit var smsReceiver: SmsReceiver
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var notificationManager: NotificationManager
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SMS_MONITORING_CHANNEL"
        private const val CHANNEL_NAME = "SMS Monitoring"
        
        fun startService(context: Context) {
            val intent = Intent(context, SmsMonitoringService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, SmsMonitoringService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification manager and create notification channel
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Initialize wake lock to keep service running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AwayText::SmsMonitoringWakeLock"
        )
        
        // Initialize SMS receiver
        smsReceiver = SmsReceiver()
        
        // Register SMS receiver
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(smsReceiver, filter)
        
        // Start DND monitoring when service starts
        DndManager.startMonitoring(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Acquire wake lock
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10*60*1000L /*10 minutes*/)
        }
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister SMS receiver
        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        
        // Stop DND monitoring when service stops
        DndManager.stopMonitoring(this)
        
        // Release wake lock
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Away Text is actively monitoring SMS messages"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Away Text Active")
            .setContentText("Monitoring messages for auto-replies")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    inner class SmsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle.get("pdus") as Array<*>?
                    if (pdus != null) {
                        for (pdu in pdus) {
                            val format = bundle.getString("format")
                            val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray, format)
                            val messageBody = smsMessage.messageBody
                            val sender = smsMessage.displayOriginatingAddress
                            
                            // Process the incoming SMS
                            handleIncomingSms(sender, messageBody)
                        }
                    }
                }
            }
        }
    }
    
    private fun handleIncomingSms(sender: String, messageBody: String) {
        // Check if app is supposed to be running
        val appPreferences = AppPreferences.getInstance(this)
        if (!appPreferences.isAppRunning) {
            return
        }
        
        // Get the appropriate message to send
        val autoReplyMessage = if (appPreferences.useCustomContactMessages && appPreferences.isContactsPermissionGranted) {
            // Try to get custom message for this contact
            appPreferences.getCustomMessageForContact(sender) ?: appPreferences.defaultAwayMessage
        } else {
            // Use default message
            appPreferences.defaultAwayMessage
        }
        
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(sender, null, autoReplyMessage, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
