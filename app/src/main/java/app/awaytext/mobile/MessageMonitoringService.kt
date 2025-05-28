package app.awaytext.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Enhanced message monitoring service that handles both SMS and RCS messages
 */
class MessageMonitoringService : Service() {
    
    private lateinit var messageReceiver: UnifiedMessageReceiver
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var notificationManager: NotificationManager
    private lateinit var rcsHandler: RcsMessageHandler
    private var rcsContentObserver: ContentObserver? = null
    
    companion object {
        private const val TAG = "MessageMonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "MESSAGE_MONITORING_CHANNEL"
        private const val CHANNEL_NAME = "Message Monitoring"
        
        fun startService(context: Context) {
            val intent = Intent(context, MessageMonitoringService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, MessageMonitoringService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Message monitoring service created")
        
        // Initialize notification manager and create notification channel
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Initialize wake lock to keep service running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AwayText::MessageMonitoringWakeLock"
        )
        
        // Initialize RCS handler
        rcsHandler = RcsMessageHandler(this)
        
        // Initialize message receiver
        messageReceiver = UnifiedMessageReceiver()
        
        // Register message receivers
        registerMessageReceivers()
        
        // Setup RCS content observer if RCS is available
        setupRcsContentObserver()
        
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
        
        // Handle specific message processing if triggered by receiver
        intent?.let { processIncomingMessage(it) }
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        Log.d(TAG, "Message monitoring service destroyed")
        
        // Unregister message receiver
        try {
            unregisterReceiver(messageReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver might not be registered: ${e.message}")
        }
        
        // Unregister RCS content observer
        rcsContentObserver?.let {
            contentResolver.unregisterContentObserver(it)
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
    
    private fun registerMessageReceivers() {
        // Register SMS receiver
        val smsFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(messageReceiver, smsFilter)
        
        // Register MMS receiver
        val mmsFilter = IntentFilter(Telephony.Mms.Intents.CONTENT_CHANGED_ACTION)
        registerReceiver(messageReceiver, mmsFilter)
        
        Log.d(TAG, "Message receivers registered")
    }
    
    private fun setupRcsContentObserver() {
        if (rcsHandler.isRcsAvailable()) {
            rcsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    Log.d(TAG, "RCS content changed: $uri")
                    checkForNewRcsMessages()
                }
            }
            
            try {
                // Register content observer for RCS messages
                contentResolver.registerContentObserver(
                    Uri.parse("content://rcs/message"),
                    true,
                    rcsContentObserver!!
                )
                Log.d(TAG, "RCS content observer registered")
            } catch (e: Exception) {
                Log.w(TAG, "Unable to register RCS content observer: ${e.message}")
            }
        }
    }
    
    private fun processIncomingMessage(intent: Intent) {
        val messageType = intent.getStringExtra("message_type")
        val sender = intent.getStringExtra("sender")
        val body = intent.getStringExtra("body")
        val checkMmsRcs = intent.getBooleanExtra("check_mms_rcs", false)
        
        when {
            messageType == "SMS" && sender != null && body != null -> {
                handleIncomingMessage(sender, body, MessageType.SMS)
            }
            checkMmsRcs -> {
                checkForNewRcsMessages()
                checkForNewMmsMessages()
            }
        }
    }
    
    private fun checkForNewRcsMessages() {
        try {
            val latestRcsMessage = rcsHandler.getLatestUnreadRcsMessage()
            latestRcsMessage?.let { message ->
                Log.d(TAG, "New RCS message from: ${message.address}")
                handleIncomingMessage(message.address, message.body, MessageType.RCS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking RCS messages: ${e.message}")
        }
    }
    
    private fun checkForNewMmsMessages() {
        // MMS handling can be added here
        // For now, we'll focus on SMS and RCS
        Log.d(TAG, "MMS message checking (placeholder)")
    }
    
    private fun handleIncomingMessage(sender: String, messageBody: String, messageType: MessageType) {
        // Check if app is supposed to be running
        val appPreferences = AppPreferences.getInstance(this)
        if (!appPreferences.isAppRunning) {
            Log.d(TAG, "App not running, ignoring message")
            return
        }
        
        Log.d(TAG, "Processing ${messageType.name} message from: $sender")
        
        // Check if we should send a reply based on the cooldown period
        if (appPreferences.canSendReplyToSender(sender)) {
            // Send auto-reply based on message type
            sendAutoReply(sender, messageType)
            // Record the timestamp of this reply
            appPreferences.recordLastReplyTimestamp(sender)
            Log.d(TAG, "Auto-reply sent and timestamp recorded for $sender")
        } else {
            // Get remaining cooldown time for better logging
            val remainingTime = appPreferences.getRemainingCooldownTime(sender)
            val formattedTime = RcsUtils.formatDuration(remainingTime)
            Log.d(TAG, "Skipping auto-reply to $sender - within cooldown period ($formattedTime remaining)")
        }
    }
    
    private fun sendAutoReply(sender: String, messageType: MessageType) {
        val appPreferences = AppPreferences.getInstance(this)
        
        // Get the appropriate message to send
        val autoReplyMessage = if (appPreferences.useCustomContactMessages && appPreferences.isContactsPermissionGranted) {
            // Try to get custom message for this contact
            appPreferences.getCustomMessageForContact(sender) ?: appPreferences.defaultAwayMessage
        } else {
            // Use default message
            appPreferences.defaultAwayMessage
        }
        
        try {
            val success = when (messageType) {
                MessageType.RCS -> {
                    Log.d(TAG, "Sending RCS auto-reply to: $sender")
                    rcsHandler.sendRcsReply(sender, autoReplyMessage)
                }
                MessageType.SMS -> {
                    Log.d(TAG, "Sending SMS auto-reply to: $sender")
                    // Use RCS handler which falls back to SMS if RCS not available
                    rcsHandler.sendRcsReply(sender, autoReplyMessage)
                }
                MessageType.MMS -> {
                    Log.d(TAG, "Sending MMS auto-reply to: $sender")
                    // For now, fallback to SMS
                    rcsHandler.sendRcsReply(sender, autoReplyMessage)
                }
            }
            
            if (success) {
                Log.i(TAG, "${messageType.name} auto-reply sent successfully to: $sender")
            } else {
                Log.w(TAG, "Failed to send ${messageType.name} auto-reply to: $sender")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending auto-reply: ${e.message}")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Away Text is actively monitoring SMS/RCS messages"
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
        
        val rcsStatus = if (rcsHandler.isRcsAvailable()) "SMS/RCS" else "SMS"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Away Text Active")
            .setContentText("Monitoring $rcsStatus messages for auto-replies")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

/**
 * Enum for different message types
 */
enum class MessageType {
    SMS,
    MMS,
    RCS
}
