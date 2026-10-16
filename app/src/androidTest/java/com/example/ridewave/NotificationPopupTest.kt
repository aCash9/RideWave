package com.example.ridewave

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ridewave.notifications.DeviceNotification
import com.example.ridewave.ui.NotificationPopup
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationPopupTest {
    @get:Rule val compose = createComposeRule()

    @Test fun idlePopupClosesAndTouchRestartsTimeout() {
        val open = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            if (open.value) NotificationPopup(true, true,
                listOf(DeviceNotification("1", "Messages", "Hello", "See you soon")),
                { open.value = false }, {})
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText("See you soon").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText("Notifications").assertExists()
        compose.mainClock.advanceTimeBy(2_100)
        compose.onNodeWithText("Notifications").assertDoesNotExist()
    }

    @Test fun permissionPromptDoesNotExposeCachedNotifications() {
        var requests = 0
        compose.setContent {
            NotificationPopup(false, true,
                listOf(DeviceNotification("1", "Messages", "Private", "Hidden")),
                {}, { requests++ })
        }
        compose.onNodeWithText("Hidden").assertDoesNotExist()
        compose.onNodeWithText("Enable notification access").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test fun emptyListAndCloseAreAvailable() {
        var dismisses = 0
        compose.setContent { NotificationPopup(true, true, emptyList(), { dismisses++ }, {}) }
        compose.onNodeWithText("No active notifications").assertExists()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals(1, dismisses) }
    }

    @Test fun closeStaysVisibleWhenNotificationsScroll() {
        val notifications = (1..20).map {
            DeviceNotification(it.toString(), "Messages", "Message $it", "Notification body $it")
        }
        compose.setContent { NotificationPopup(true, true, notifications, {}, {}) }

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(19)

        compose.onNodeWithText("Close").assertIsDisplayed()
        compose.onNodeWithText("Notifications").assertIsDisplayed()
        compose.onNodeWithText("Notification body 20").assertIsDisplayed()
    }
}
