package app.awaytext.mobile

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.awaytext.mobile.ui.theme.AwayTextTheme

@Composable
fun WelcomeIntroScreen(onNextClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Away Text",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Image(
            painter = painterResource(id = R.drawable.welcome_image),
            contentDescription = "Welcome Image",
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Your personal messaging companion",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To get started, we'll need to set up a few permissions to help you manage your messages automatically.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun SmsPermissionScreen(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    
    val smsPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS
    )
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            AppPreferences.getInstance(context).setSmsPermissionsGranted(true)
            onPermissionGranted()
        }
    }
    
    PermissionScreenTemplate(
        icon = Icons.Default.Email,
        title = "SMS & RCS Permissions",
        description = "Allow Away Text to read and send SMS and RCS messages to automatically respond when you're away.",
        details = listOf(
            "Read incoming SMS and RCS messages to detect when you're being contacted",
            "Send automatic replies using SMS or RCS when you're unavailable",
            "Monitor message notifications across all messaging types",
            "Automatically choose the best messaging format (RCS when available, SMS as fallback)"
        ),
        onGrantPermissionClick = {
            launcher.launch(smsPermissions)
        },
        onSkipClick = onSkip,
        isPermissionGranted = smsPermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }
    )
}

@Composable
fun ContactsPermissionScreen(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AppPreferences.getInstance(context).setContactsPermissionGranted(true)
            onPermissionGranted()
        }
    }
    
    PermissionScreenTemplate(
        icon = Icons.Default.Person,
        title = "Contacts Access",
        description = "Access your contacts to customize automatic responses for specific people.",
        details = listOf(
            "Personalize responses for different contacts",
            "Set different auto-reply rules for work vs. personal contacts",
            "Identify important contacts for priority handling"
        ),
        onGrantPermissionClick = {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        },
        onSkipClick = onSkip,
        isPermissionGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED,
        isOptional = true
    )
}

@Composable
fun NotificationPolicyScreen(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            val appPreferences = AppPreferences.getInstance(context)
            appPreferences.setNotificationPolicyGranted(true)
            // Enable DND auto-start by default when permission is granted
            appPreferences.setDndAutoStartEnabled(true)
            onPermissionGranted()
        }
    }
    
    PermissionScreenTemplate(
        icon = Icons.Default.Notifications,
        title = "Do Not Disturb Access",
        description = "Allow Away Text to work with your Do Not Disturb settings for intelligent auto-replies.",
        details = listOf(
            "Automatically activate when Do Not Disturb is enabled",
            "Respect your quiet hours and focus time",
            "Integrate with your existing notification preferences"
        ),
        onGrantPermissionClick = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            launcher.launch(intent)
        },
        onSkipClick = onSkip,
        isPermissionGranted = run {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.isNotificationPolicyAccessGranted
        },
        isOptional = true
    )
}

@Composable
fun BackgroundPermissionScreen(
    onPermissionGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check if battery optimization is disabled and wake lock permission is granted
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatteryOptimizationDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
        
        if (isBatteryOptimizationDisabled) {
            AppPreferences.getInstance(context).setBackgroundPermissionGranted(true)
            onPermissionGranted()
        }
    }
    
    PermissionScreenTemplate(
        icon = Icons.Default.Settings,
        title = "Background Running",
        description = "Allow Away Text to run continuously in the background to monitor messages 24/7.",
        details = listOf(
            "Keep the app active when your phone is locked",
            "Monitor messages even when other apps are running",
            "Ensure reliable auto-reply functionality",
            "Optimize battery usage while staying responsive"
        ),
        onGrantPermissionClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                launcher.launch(intent)
            } else {
                AppPreferences.getInstance(context).setBackgroundPermissionGranted(true)
                onPermissionGranted()
            }
        },
        onSkipClick = onSkip,
        isPermissionGranted = run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        },
        isOptional = true
    )
}

@Composable
private fun PermissionScreenTemplate(
    icon: ImageVector,
    title: String,
    description: String,
    details: List<String>,
    onGrantPermissionClick: () -> Unit,
    onSkipClick: () -> Unit,
    isPermissionGranted: Boolean,
    isOptional: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "This allows Away Text to:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                details.forEach { detail ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = detail,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (isPermissionGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Permission granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permission granted!",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Button(
                onClick = onSkipClick, // Call onSkip to proceed to next step
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 18.sp
                )
            }
        } else {
            Button(
                onClick = onGrantPermissionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Grant Permission",
                    fontSize = 18.sp
                )
            }
            
            if (isOptional) {
                TextButton(
                    onClick = onSkipClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for now",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingCompleteScreen(onFinishClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Complete",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "All Set!",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Away Text is ready to help you manage your messages automatically.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onFinishClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Start Using Away Text",
                fontSize = 18.sp
            )
        }
    }
}
