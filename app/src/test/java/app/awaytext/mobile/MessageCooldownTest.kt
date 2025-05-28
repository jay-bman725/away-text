package app.awaytext.mobile

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test case for the message cooldown functionality
 */
@RunWith(MockitoJUnitRunner::class)
class MessageCooldownTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var appPreferences: AppPreferences
    
    private val testSender = "+1234567890"
    private val normalizedTestSender = "1234567890" // Normalized version without the +
    private val testTimestamp = 1000000000000L // Example timestamp
    
    @Before
    fun setup() {
        `when`(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPrefs)
        `when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putLong(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putStringSet(any(), any())).thenReturn(mockEditor)
        
        // Initialize AppPreferences with mocked context
        appPreferences = spy(AppPreferences.getInstance(mockContext))
    }
    
    @Test
    fun `test canSendReplyToSender returns true when no previous message sent`() {
        // Setup: No previous message timestamp
        `when`(mockSharedPrefs.getLong(contains("lastReplyTimestamp"), any())).thenReturn(0L)
        
        // Test
        val result = appPreferences.canSendReplyToSender(testSender)
        
        // Verify
        assertTrue(result, "Should be able to send a message when no previous message was sent")
        verify(mockSharedPrefs).getLong(contains("lastReplyTimestamp_$normalizedTestSender"), eq(0L))
    }
    
    @Test
    fun `test canSendReplyToSender returns false when previous message sent within cooldown`() {
        // Setup: Previous message was sent within cooldown period
        val now = System.currentTimeMillis()
        val recentTimestamp = now - TimeUnit.MINUTES.toMillis(2) // 2 minutes ago
        `when`(mockSharedPrefs.getLong(contains("lastReplyTimestamp"), any())).thenReturn(recentTimestamp)
        
        // Test
        val result = appPreferences.canSendReplyToSender(testSender)
        
        // Verify
        assertFalse(result, "Should not be able to send a message when previous message was sent within cooldown")
    }
    
    @Test
    fun `test canSendReplyToSender returns true when previous message sent outside cooldown`() {
        // Setup: Previous message was sent outside cooldown period
        val now = System.currentTimeMillis()
        val oldTimestamp = now - TimeUnit.MINUTES.toMillis(6) // 6 minutes ago (outside 5-min cooldown)
        `when`(mockSharedPrefs.getLong(contains("lastReplyTimestamp"), any())).thenReturn(oldTimestamp)
        
        // Test
        val result = appPreferences.canSendReplyToSender(testSender)
        
        // Verify
        assertTrue(result, "Should be able to send a message when previous message was sent outside cooldown")
    }
    
    @Test
    fun `test recordLastReplyTimestamp saves timestamp correctly`() {
        // Setup
        `when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        
        // Test
        appPreferences.recordLastReplyTimestamp(testSender)
        
        // Verify
        verify(mockEditor).putLong(contains("lastReplyTimestamp_$normalizedTestSender"), any())
        verify(mockEditor).apply()
    }
    
    @Test
    fun `test getRemainingCooldownTime returns correct value`() {
        // Setup: Previous message sent 2 minutes ago (3 minutes remaining in cooldown)
        val now = System.currentTimeMillis()
        val twoMinutesAgo = now - TimeUnit.MINUTES.toMillis(2) 
        `when`(mockSharedPrefs.getLong(contains("lastReplyTimestamp"), any())).thenReturn(twoMinutesAgo)
        
        // Test
        val remainingTime = appPreferences.getRemainingCooldownTime(testSender)
        
        // Verify - should be roughly 3 minutes (with some tolerance for test execution time)
        assertTrue(remainingTime > TimeUnit.MINUTES.toMillis(2) && 
                   remainingTime <= TimeUnit.MINUTES.toMillis(3),
                  "Remaining time should be about 3 minutes")
    }
    
    @Test
    fun `test getRemainingCooldownTime returns 0 when cooldown elapsed`() {
        // Setup: Previous message sent 6 minutes ago (outside cooldown)
        val now = System.currentTimeMillis()
        val sixMinutesAgo = now - TimeUnit.MINUTES.toMillis(6) 
        `when`(mockSharedPrefs.getLong(contains("lastReplyTimestamp"), any())).thenReturn(sixMinutesAgo)
        
        // Test
        val remainingTime = appPreferences.getRemainingCooldownTime(testSender)
        
        // Verify
        assertEquals(0L, remainingTime, "Remaining time should be 0 when cooldown has elapsed")
    }
    
    @Test
    fun `test phone number normalization works correctly`() {
        // Setup: Different formats of the same number
        val phoneFormats = listOf(
            "+1234567890",
            "1234567890",
            "(123) 456-7890",
            "123-456-7890",
            "123.456.7890"
        )
        
        // Need to mock some implementation for our test
        `when`(mockSharedPrefs.getLong(any(), any())).thenReturn(0L)
        
        // Test all formats
        for (phone in phoneFormats) {
            appPreferences.recordLastReplyTimestamp(phone)
        }
        
        // Verify all formats were normalized to the same value
        verify(mockEditor, times(phoneFormats.size))
            .putLong(eq("lastReplyTimestamp_1234567890"), any())
    }
}
