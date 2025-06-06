package app.awaytext.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.awaytext.mobile.ui.theme.AwayTextTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start DND monitoring when the app is created
        DndManager.startMonitoring(this)
        
        // Check if DND is active and auto-start if needed
        DndManager.checkDndStateAndAutoStart(this)
        
        setContent {
            AwayTextTheme {
                var showMenu by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var showNoUpdateDialog by remember { mutableStateOf(false) }
                var updateVersion by remember { mutableStateOf("") }
                val context = LocalContext.current
                
                // Check for updates when the app starts
                LaunchedEffect(Unit) {
                    VersionChecker.checkForUpdatesIfNeeded(
                        context = context,
                        onUpdateAvailable = { version ->
                            updateVersion = version
                            showUpdateDialog = true
                        },
                        onCheckComplete = { updateFound ->
                            // Only show "no update" dialog if manually triggered
                            // This is handled in SettingsScreen when user manually checks
                        }
                    )
                }
                
                if (showSettings) {
                    SettingsScreen(
                        onNavigateBack = { showSettings = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { 
                                    Text(
                                        "Away Text",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                actions = {
                                    IconButton(onClick = { showMenu = !showMenu }) {
                                        Icon(
                                            Icons.Default.MoreVert, 
                                            contentDescription = "More options",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Reset Onboarding") },
                                            onClick = {
                                                showMenu = false
                                                // Reset all onboarding, permissions, and settings
                                                AppPreferences.getInstance(context).resetAllPermissionsAndSettings()
                                                Toast.makeText(context, "All settings reset. Restart app to see welcome screen.", Toast.LENGTH_LONG).show()
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Refresh, contentDescription = null)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
                
                // Update Available Dialog
                if (showUpdateDialog && updateVersion.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { 
                            showUpdateDialog = false
                            VersionChecker.dismissCurrentUpdate(context)
                        },
                        title = { Text("Update Available") },
                        text = { 
                            Text("A new version ($updateVersion) of Away Text is available. Would you like to download it?")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showUpdateDialog = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(VersionChecker.getReleasesUrl()))
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Download")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { 
                                    showUpdateDialog = false
                                    VersionChecker.dismissCurrentUpdate(context)
                                }
                            ) {
                                Text("Later")
                            }
                        }
                    )
                }
                
                // No Update Dialog
                if (showNoUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = { showNoUpdateDialog = false },
                        title = { Text("No Updates") },
                        text = { 
                            Text("You have the latest version of Away Text.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showNoUpdateDialog = false }
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop DND monitoring when the app is destroyed
        DndManager.stopMonitoring(this)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val appPreferences = AppPreferences.getInstance(context)
    
    // State for app running status
    var isAppRunning by remember { mutableStateOf(appPreferences.isAppRunning) }
    
    // Animation for the main icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Background gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hero Status Card with enhanced design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAppRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated status icon with background circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAppRunning) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            }
                        )
                        .scale(if (isAppRunning) pulseScale else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAppRunning) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = if (isAppRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isAppRunning) "Away Text is Active" else "Away Text is Ready",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAppRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isAppRunning) {
                        "✨ Monitoring SMS/RCS messages and sending auto-replies"
                    } else {
                        "Tap the button below to start monitoring messages"
                    },
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = if (isAppRunning) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enhanced Toggle Button
        AnimatedContent(
            targetState = isAppRunning,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "button_animation"
        ) { isRunning ->
            Button(
                onClick = {
                    val newRunningState = !isAppRunning
                    isAppRunning = newRunningState
                    appPreferences.setAppRunning(newRunningState)
                    
                    if (newRunningState) {
                        // Start the message monitoring service (SMS/RCS)
                        MessageMonitoringService.startService(context)
                        Toast.makeText(context, "Away Text started", Toast.LENGTH_SHORT).show()
                    } else {
                        // Stop the message monitoring service
                        MessageMonitoringService.stopService(context)
                        Toast.makeText(context, "Away Text stopped", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isRunning) "Stop Away Text" else "Start Away Text",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Enhanced Feature Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How it works",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val features = listOf(
                    Pair(Icons.Default.Email, "Monitors incoming SMS and RCS messages"),
                    Pair(Icons.Default.Send, "Automatically sends replies when active"),
                    Pair(Icons.Default.Settings, "Runs in the background continuously"),
                    Pair(Icons.Default.Notifications, "Respects your Do Not Disturb settings")
                )
                
                features.forEach { (icon, feature) ->
                    FeatureRow(icon = icon, text = feature)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enhanced DND Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Do Not Disturb Integration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Automatically control AwayText based on your Do Not Disturb settings",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Auto-start when DND enabled
                var isDndAutoStartEnabled by remember { 
                    mutableStateOf(appPreferences.isDndAutoStartEnabled) 
                }
                
                SettingToggleRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Auto-start with Do Not Disturb",
                    subtitle = "Start AwayText when DND is enabled",
                    checked = isDndAutoStartEnabled,
                    onCheckedChange = { enabled ->
                        isDndAutoStartEnabled = enabled
                        appPreferences.setDndAutoStartEnabled(enabled)
                        
                        if (enabled) {
                            // Start DND monitoring
                            DndManager.startMonitoring(context)
                            // Check current DND state
                            DndManager.checkDndStateAndAutoStart(context)
                            Toast.makeText(context, "DND auto-start enabled", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "DND auto-start disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Auto-stop when DND disabled
                var isDndAutoStopEnabled by remember { 
                    mutableStateOf(appPreferences.isDndAutoStopEnabled) 
                }
                
                SettingToggleRow(
                    icon = Icons.Default.Close,
                    title = "Auto-stop when DND ends",
                    subtitle = "Stop AwayText when DND is disabled",
                    checked = isDndAutoStopEnabled,
                    onCheckedChange = { enabled ->
                        isDndAutoStopEnabled = enabled
                        appPreferences.setDndAutoStopEnabled(enabled)
                        
                        if (enabled) {
                            Toast.makeText(context, "DND auto-stop enabled", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "DND auto-stop disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Show current DND status
                val isDndActive = remember { DndManager.isDndActive(context) }
                
                if (isDndActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Do Not Disturb is currently active",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // RCS Status Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = RcsUtils.getRcsShortStatus(context),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Enhanced Settings Button
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
