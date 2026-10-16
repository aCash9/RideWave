package com.example.ridewave

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ridewave.contacts.RideContact
import com.example.ridewave.ui.ContactsOverlay
import com.example.ridewave.ui.contactCallIntent
import com.example.ridewave.ui.theme.RideWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ContactsLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun callIntentPlacesTheSelectedNumberWithoutLaunchingIt() {
        val number = "+1 (202) 555-0100"
        val intent = contactCallIntent(number)
        assertEquals(Intent.ACTION_CALL, intent.action)
        assertEquals("tel", intent.data?.scheme)
        assertEquals(number, intent.data?.schemeSpecificPart)
    }

    @Test fun portraitUsesLargeTwoColumnTargets() = checkLayout(false)

    @Test fun landscapeKeepsAllTargetsVisibleWithLargeText() = checkLayout(true)

    private fun checkLayout(landscape: Boolean) {
        val orientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val expected = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        compose.runOnUiThread { compose.activity.requestedOrientation = orientation }
        compose.waitUntil(10_000) { compose.activity.resources.configuration.orientation == expected }
        val contacts = listOf(RideContact("Alex", "100"), RideContact("Jordan", "200"), RideContact("Sam", "300"))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (landscape) 1.5f else 1f)) {
                RideWaveTheme { ContactsOverlay(contacts, false, {}, {}, {}, {}, timerPaused = true) }
            }
        }
        val targets = contacts.map { compose.onNodeWithContentDescription("Call ${it.name}, ${it.number}") } +
            compose.onNodeWithContentDescription("Add contact")
        targets.forEach { it.assertIsDisplayed().assertWidthIsAtLeast(120.dp).assertHeightIsAtLeast(120.dp) }
        val boxes = targets.map { it.fetchSemanticsNode().boundsInRoot }
        if (landscape) {
            assertTrue(boxes.zipWithNext().all { (a, b) -> a.right <= b.left + 1f })
        } else {
            assertTrue(boxes[0].right <= boxes[1].left + 1f)
            assertTrue(boxes[0].bottom <= boxes[2].top + 1f)
        }
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val output = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "contacts-preview-${if (landscape) "landscape" else "portrait"}.png")
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}
