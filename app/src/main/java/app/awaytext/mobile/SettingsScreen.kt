package app.awaytext.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appPreferences = AppPreferences.getInstance(context)
    
    // State variables
    var defaultMessage by remember { mutableStateOf(appPreferences.defaultAwayMessage) }
    var useCustomContactMessages by remember { mutableStateOf(appPreferences.useCustomContactMessages) }
    var showContactPermissionDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    
    // Check contacts permission
    val hasContactsPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
    
    // Permission launcher for contacts
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            appPreferences.setContactsPermissionGranted(true)
            Toast.makeText(context, "Contacts permission granted", Toast.LENGTH_SHORT).show()
        } else {
            showContactPermissionDialog = true
        }
    }
    
    // Check for updates on screen load
    LaunchedEffect(Unit) {
        VersionChecker.checkForUpdatesIfNeeded(
            context = context,
            onUpdateAvailable = { version ->
                updateVersion = version
                showUpdateDialog = true
            }
        )
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Away Message Settings Card
            SettingsCard(
                title = "Away Message",
                icon = Icons.Default.Email
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Default message sent to all numbers",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = defaultMessage,
                        onValueChange = { 
                            defaultMessage = it
                            appPreferences.defaultAwayMessage = it
                        },
                        label = { Text("Default Away Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            // Custom Contact Messages Card
            SettingsCard(
                title = "Custom Contact Messages",
                icon = Icons.Default.Person
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Send personalized messages to specific contacts",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Enable custom messages",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasContactsPermission) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                            if (!hasContactsPermission) {
                                Text(
                                    text = "Contacts permission required",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        Switch(
                            checked = useCustomContactMessages && hasContactsPermission,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasContactsPermission) {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                } else {
                                    useCustomContactMessages = enabled
                                    appPreferences.useCustomContactMessages = enabled
                                }
                            },
                            enabled = hasContactsPermission
                        )
                    }
                    
                    if (useCustomContactMessages && hasContactsPermission) {
                        CustomContactMessagesSection(appPreferences = appPreferences)
                    }
                    
                    if (!hasContactsPermission) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Grant contacts permission to enable custom messages",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            
            // App Information Card
            SettingsCard(
                title = "App Information",
                icon = Icons.Default.Info
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val currentVersion = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Version",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentVersion,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    OutlinedButton(
                        onClick = {
                            VersionChecker.checkForUpdates(
                                context = context,
                                onUpdateAvailable = { version ->
                                    updateVersion = version
                                    showUpdateDialog = true
                                },
                                onCheckComplete = { updateFound ->
                                    if (!updateFound) {
                                        showNoUpdateDialog = true
                                    }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Contact Permission Dialog
    if (showContactPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showContactPermissionDialog = false },
            title = { Text("Contacts Permission Required") },
            text = { 
                Text("To use custom contact messages, Away Text needs permission to read your contacts. This allows the app to identify specific contacts and send personalized messages.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showContactPermissionDialog = false
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showContactPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
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
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            content()
        }
    }
}

@Composable
fun CustomContactMessagesSection(
    appPreferences: AppPreferences
) {
    val context = LocalContext.current
    var customMessages by remember { mutableStateOf(appPreferences.getAllCustomContactMessages()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<ContactUtils.Contact?>(null) }
    var newMessage by remember { mutableStateOf("") }
    var contactSearchQuery by remember { mutableStateOf("") }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (customMessages.isNotEmpty()) {
            Text(
                text = "Custom messages (${customMessages.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            customMessages.forEach { (phoneNumber, message) ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Try to get contact name, fallback to phone number
                            val contact = ContactUtils.getContactByPhoneNumber(context, phoneNumber)
                            val displayName = contact?.name ?: phoneNumber
                            
                            Text(
                                text = displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (contact != null) {
                                Text(
                                    text = phoneNumber,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                appPreferences.removeCustomContactMessage(phoneNumber)
                                customMessages = appPreferences.getAllCustomContactMessages()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        OutlinedButton(
            onClick = { showContactPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Custom Message")
        }
    }

    // Contact Picker Dialog
    if (showContactPicker) {
        ContactPickerDialog(
            onContactSelected = { contact ->
                // Check if contact already has a custom message
                val phoneNumber = contact.getPrimaryPhoneNumber()
                if (phoneNumber != null && appPreferences.getCustomMessageForContact(phoneNumber) != null) {
                    // Contact already has a custom message
                    Toast.makeText(
                        context, 
                        "This contact already has a custom message. Please delete the existing message first.", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    selectedContact = contact
                    showAddDialog = true
                }
                showContactPicker = false
                contactSearchQuery = ""
            },
            onDismiss = { 
                showContactPicker = false
                contactSearchQuery = ""
            },
            searchQuery = contactSearchQuery,
            onSearchQueryChange = { contactSearchQuery = it },
            existingCustomMessages = customMessages
        )
    }
    
    // Add Custom Message Dialog
    if (showAddDialog && selectedContact != null) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                selectedContact = null
                newMessage = ""
            },
            title = { Text("Add Custom Message") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Show selected contact info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedContact!!.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedContact!!.getPrimaryPhoneNumber() ?: "No phone number",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        label = { Text("Custom Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phoneNumber = selectedContact!!.getPrimaryPhoneNumber()
                        if (phoneNumber != null && newMessage.isNotBlank()) {
                            appPreferences.addCustomContactMessage(phoneNumber, newMessage.trim())
                            customMessages = appPreferences.getAllCustomContactMessages()
                            showAddDialog = false
                            selectedContact = null
                            newMessage = ""
                            Toast.makeText(context, "Custom message added", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedContact?.getPrimaryPhoneNumber() != null && newMessage.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddDialog = false
                        selectedContact = null
                        newMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContactPickerDialog(
    onContactSelected: (ContactUtils.Contact) -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    existingCustomMessages: Map<String, String>
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactUtils.Contact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load contacts when dialog opens
    LaunchedEffect(Unit) {
        isLoading = true
        contacts = ContactUtils.getAllContactsWithPhoneNumbers(context)
        isLoading = false
    }
    
    // Filter contacts based on search query
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.phoneNumbers.any { it.contains(searchQuery) }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Contact") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search contacts") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No contacts found" else "No matching contacts",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredContacts) { contact ->
                            val hasCustomMessage = contact.getPrimaryPhoneNumber()?.let { phoneNumber ->
                                existingCustomMessages.containsKey(phoneNumber)
                            } ?: false
                            
                            ContactItem(
                                contact = contact,
                                hasCustomMessage = hasCustomMessage,
                                onClick = { onContactSelected(contact) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ContactItem(
    contact: ContactUtils.Contact,
    hasCustomMessage: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !hasCustomMessage) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (hasCustomMessage) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasCustomMessage) Icons.Default.Check else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (hasCustomMessage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (hasCustomMessage) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (contact.phoneNumbers.size == 1) {
                    Text(
                        text = contact.phoneNumbers.first(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${contact.phoneNumbers.size} phone numbers",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (hasCustomMessage) {
                    Text(
                        text = "Custom message already exists",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
