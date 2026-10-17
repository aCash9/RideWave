package com.example.ridewave

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ridewave.contacts.RideContact
import com.example.ridewave.ui.ContactsOverlay
import com.example.ridewave.ui.RidePage
import com.example.ridewave.ui.RideWaveScreen
import com.example.ridewave.gestures.GestureDetector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class ContactsOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyOverlayOffersAdd() {
        var adds = 0
        compose.setContent { ContactsOverlay(emptyList(), false, {}, { adds++ }, {}, {}) }
        compose.onNodeWithText("RIDE CONTACTS").assertIsDisplayed()
        compose.onNodeWithText("Tap a contact", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Add contact")
            .assertWidthIsAtLeast(96.dp)
            .assertHeightIsAtLeast(96.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, adds) }
    }

    @Test fun fourContactsHideAddAndSupportCallingAndRemoval() {
        val contacts = (1..4).map { RideContact("Person $it", "555000$it") }
        var called: RideContact? = null
        var removed: RideContact? = null
        compose.setContent {
            ContactsOverlay(contacts, false, {}, {}, { called = it }, { removed = it })
        }
        compose.onNodeWithContentDescription("Add contact").assertDoesNotExist()
        contacts.forEach { contact ->
            compose.onNodeWithContentDescription("Call ${contact.name}, ${contact.number}")
                .assertIsDisplayed()
                .assertWidthIsAtLeast(96.dp)
                .assertHeightIsAtLeast(96.dp)
        }
        compose.onNodeWithContentDescription("Call Person 1, 5550001").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("Remove Person 4").performClick()
        compose.runOnIdle {
            assertEquals(contacts.first(), called)
            assertEquals(contacts.last(), removed)
        }
    }

    @Test fun photographedContactShowsOnlyTheEdgeToEdgeImage() {
        val photo = ByteArrayOutputStream().use { output ->
            Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).run {
                eraseColor(android.graphics.Color.BLUE)
                compress(Bitmap.CompressFormat.JPEG, 90, output)
                recycle()
            }
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
        val contact = RideContact("Photo Contact", "5550100", photo)
        compose.setContent { ContactsOverlay(listOf(contact), false, {}, {}, {}, {}) }

        compose.onNodeWithContentDescription("Call Photo Contact, 5550100").assertIsDisplayed()
        compose.onNodeWithText("Photo Contact").assertDoesNotExist()
    }

    @Test fun idleOverlayClosesAfterTenSeconds() {
        val open = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            if (open.value) ContactsOverlay(emptyList(), false, { open.value = false }, {}, {}, {})
        }
        compose.mainClock.advanceTimeBy(9_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("RIDE CONTACTS").assertDoesNotExist()
    }

    @Test fun touchingOverlayRestartsTenSecondTimeout() {
        val open = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            if (open.value) ContactsOverlay(emptyList(), false, { open.value = false }, {}, {}, {})
        }
        compose.mainClock.advanceTimeBy(7_000)
        compose.onNodeWithContentDescription("Add contact").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(7_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.mainClock.advanceTimeBy(3_500)
        compose.onNodeWithText("RIDE CONTACTS").assertDoesNotExist()
    }

    @Test fun accessibilityClickRestartsTenSecondTimeout() {
        val open = mutableStateOf(true)
        var adds = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            if (open.value) ContactsOverlay(emptyList(), false, { open.value = false }, { adds++ }, {}, {})
        }
        compose.mainClock.advanceTimeBy(7_000)
        compose.onNodeWithContentDescription("Add contact").performClick()
        compose.mainClock.advanceTimeBy(7_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.runOnIdle { assertEquals(1, adds) }
        compose.mainClock.advanceTimeBy(3_500)
        compose.onNodeWithText("RIDE CONTACTS").assertDoesNotExist()
    }

    @Test fun pausedPickerAndBusyStorageEachSuspendTheTimeout() {
        val open = mutableStateOf(true)
        val paused = mutableStateOf(true)
        val busy = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            if (open.value) ContactsOverlay(emptyList(), busy.value, { open.value = false }, {}, {}, {},
                timerPaused = paused.value)
        }
        compose.mainClock.advanceTimeBy(12_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.runOnIdle {
            paused.value = false
            busy.value = true
        }
        compose.mainClock.advanceTimeBy(12_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.runOnIdle { busy.value = false }
        compose.mainClock.advanceTimeBy(9_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("RIDE CONTACTS").assertDoesNotExist()
    }

    @Test fun backgroundingPausesTimeoutAndResumingRestartsIt() {
        val open = mutableStateOf(true)
        lateinit var owner: OverlayLifecycleOwner
        compose.runOnUiThread {
            owner = OverlayLifecycleOwner().apply { registry.currentState = Lifecycle.State.RESUMED }
        }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                if (open.value) ContactsOverlay(emptyList(), false, { open.value = false }, {}, {}, {})
            }
        }
        compose.mainClock.advanceTimeBy(7_000)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeBy(12_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(9_000)
        compose.onNodeWithText("RIDE CONTACTS").assertExists()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("RIDE CONTACTS").assertDoesNotExist()
    }

    @Test fun contactControlsDoNotTriggerPlayback() {
        var gestures = 0
        compose.setContent {
            RideWaveScreen(RidePage.RIDE, "09:41", true, false, null,
                {}, {}, {}, { gestures++ }, GestureDetector(), {})
        }
        compose.onNodeWithContentDescription("Contacts").performTouchInput { click(center) }
        compose.onNodeWithText("RIDE CONTACTS").assertIsDisplayed()
        compose.onNode(hasText("Close") or hasContentDescription("Close contacts")).performClick()
        compose.onNodeWithText("RIDE CONTACTS").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, gestures) }
    }
}

private class OverlayLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}
