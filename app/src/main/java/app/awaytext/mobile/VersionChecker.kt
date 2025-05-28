package app.awaytext.mobile

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class for checking app version updates
 */
object VersionChecker {
    private const val TAG = "VersionChecker"
    private const val VERSION_URL = "https://raw.githubusercontent.com/jay-bman725/away-text/refs/heads/main/version"
    private const val RELEASES_URL = "https://github.com/jay-bman725/away-text/releases"
    private const val CHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour in milliseconds
    
    /**
     * Check for app updates if needed (respects the 1-hour interval)
     */
    fun checkForUpdatesIfNeeded(context: Context, onUpdateAvailable: (String) -> Unit, onCheckComplete: ((Boolean) -> Unit)? = null) {
        val prefs = AppPreferences.getInstance(context)
        val now = System.currentTimeMillis()
        
        // Check if enough time has passed since last check
        if (now - prefs.lastVersionCheck < CHECK_INTERVAL_MS) {
            Log.d(TAG, "Skipping version check - too soon since last check")
            onCheckComplete?.invoke(false)
            return
        }
        
        // Update last check time
        prefs.lastVersionCheck = now
        
        // Perform the version check
        checkForUpdates(context, onUpdateAvailable, onCheckComplete)
    }
    
    /**
     * Force check for app updates
     */
    fun checkForUpdates(context: Context, onUpdateAvailable: (String) -> Unit, onCheckComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val remoteVersion = fetchRemoteVersion()
                var updateFound = false
                
                if (remoteVersion != null) {
                    val currentVersion = getCurrentVersion(context)
                    val prefs = AppPreferences.getInstance(context)
                    
                    // Store the latest known version
                    prefs.latestKnownVersion = remoteVersion
                    
                    if (isNewerVersion(currentVersion, remoteVersion)) {
                        updateFound = true
                        // Reset dismissed status for new versions
                        if (prefs.latestKnownVersion != remoteVersion) {
                            prefs.isVersionUpdateDismissed = false
                        }
                        
                        // Only notify if user hasn't dismissed this version
                        if (!prefs.isVersionUpdateDismissed) {
                            withContext(Dispatchers.Main) {
                                onUpdateAvailable(remoteVersion)
                            }
                        }
                    }
                }
                
                // Notify completion with result
                withContext(Dispatchers.Main) {
                    onCheckComplete?.invoke(updateFound)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates: ${e.message}")
                withContext(Dispatchers.Main) {
                    onCheckComplete?.invoke(false)
                }
            }
        }
    }
    
    /**
     * Fetch the remote version from GitHub
     */
    private suspend fun fetchRemoteVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(VERSION_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000 // 10 seconds
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val version = reader.readLine()?.trim()
                reader.close()
                Log.d(TAG, "Remote version: $version")
                version
            } else {
                Log.w(TAG, "Failed to fetch remote version: HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote version: ${e.message}")
            null
        }
    }
    
    /**
     * Get the current app version
     */
    private fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current version: ${e.message}")
            "0.0"
        }
    }
    
    /**
     * Compare version strings to determine if remote is newer
     * Supports formats like "1.2", "1.2.3", etc.
     */
    private fun isNewerVersion(current: String, remote: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(currentParts.size, remoteParts.size)
            
            for (i in 0 until maxLength) {
                val currentPart = currentParts.getOrNull(i) ?: 0
                val remotePart = remoteParts.getOrNull(i) ?: 0
                
                when {
                    remotePart > currentPart -> return true
                    remotePart < currentPart -> return false
                    // Continue to next part if equal
                }
            }
            
            // Versions are equal
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions: ${e.message}")
            return false
        }
    }
    
    /**
     * Get the releases URL
     */
    fun getReleasesUrl(): String = RELEASES_URL
    
    /**
     * Mark the current version update as dismissed
     */
    fun dismissCurrentUpdate(context: Context) {
        val prefs = AppPreferences.getInstance(context)
        prefs.isVersionUpdateDismissed = true
    }
}
