package com.example.ridewave

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class RotationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun orientationChangesKeepTheActivityAndItsSpotifyManager() {
        val original = compose.activity
        for ((orientation, expected) in listOf(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to Configuration.ORIENTATION_PORTRAIT
        )) {
            compose.runOnUiThread { original.requestedOrientation = orientation }
            compose.waitUntil(10_000) {
                compose.activity.resources.configuration.orientation == expected
            }
            compose.waitForIdle()
            assertSame(original, compose.activity)
        }
    }
}
