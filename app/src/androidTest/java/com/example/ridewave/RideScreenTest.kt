package com.example.ridewave

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.example.ridewave.gestures.GestureDetector
import com.example.ridewave.gestures.GestureType
import com.example.ridewave.ui.RidePage
import com.example.ridewave.ui.RideWaveScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RideScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ridePadMapsTapAndBothSwipes() {
        val gestures = mutableListOf<GestureType>()
        compose.setContent {
            RideWaveScreen(RidePage.RIDE, "09:41", true, false, null,
                {}, {}, {}, { gestures.add(it) }, GestureDetector(), {})
        }
        val pad = compose.onNodeWithContentDescription("Ride controls")
        pad.performTouchInput { click(center) }
        pad.performTouchInput { swipeLeft() }
        pad.performTouchInput { swipeRight() }
        compose.runOnIdle {
            assertEquals(listOf(GestureType.SINGLE_TAP, GestureType.SWIPE_LEFT, GestureType.SWIPE_RIGHT), gestures)
        }
    }

    @Test fun notificationsDoNotTriggerPlayback() {
        var gestures = 0
        compose.setContent {
            RideWaveScreen(RidePage.RIDE, "09:41", true, false, null,
                {}, {}, {}, { gestures++ }, GestureDetector(), {})
        }
        compose.onNodeWithContentDescription("Notifications").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(0, gestures) }
    }

    @Test fun closeDoesNotTriggerPlayback() {
        var closes = 0
        var gestures = 0
        compose.setContent {
            RideWaveScreen(RidePage.RIDE, "09:41", true, false, null,
                {}, {}, {}, { gestures++ }, GestureDetector(), { closes++ })
        }
        compose.onNodeWithContentDescription("Close RideWave").performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(1, closes)
            assertEquals(0, gestures)
        }
    }
}
