package app.awaytext.mobile

import android.app.NotificationManager
import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for DND functionality
 */
@RunWith(MockitoJUnitRunner::class)
class DndManagerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var notificationManager: NotificationManager

    @Mock
    private lateinit var appPreferences: AppPreferences

    @Test
    fun `isDndActive returns true when DND is enabled`() {
        // Given
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(true)
        `when`(notificationManager.currentInterruptionFilter).thenReturn(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

        // When
        val result = DndManager.isDndActive(context)

        // Then
        assert(result)
    }

    @Test
    fun `isDndActive returns false when DND is disabled`() {
        // Given
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(true)
        `when`(notificationManager.currentInterruptionFilter).thenReturn(NotificationManager.INTERRUPTION_FILTER_ALL)

        // When
        val result = DndManager.isDndActive(context)

        // Then
        assert(!result)
    }

    @Test
    fun `isDndActive returns false when notification policy access not granted`() {
        // Given
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(false)

        // When
        val result = DndManager.isDndActive(context)

        // Then
        assert(!result)
    }
}
