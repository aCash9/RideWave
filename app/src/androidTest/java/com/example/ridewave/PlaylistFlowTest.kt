package com.example.ridewave

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.ridewave.spotify.SpotifyWebApi
import com.example.ridewave.ui.PlaylistButton
import com.example.ridewave.ui.theme.RideWaveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val playlists = (1..5).map {
        SpotifyWebApi.Playlist("$it", "Playlist $it", "spotify:playlist:$it")
    }

    @Test fun doneSavesFourAndEditCanReplaceWithoutPlaying() {
        var saved = emptySet<String>()
        var played: String? = null
        compose.setContent {
            var ids by remember { mutableStateOf(emptySet<String>()) }
            RideWaveTheme {
                PlaylistButton(playlists, false, true, ids, {}, {},
                    { ids = it; saved = it }, { played = it.id })
            }
        }
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.onNodeWithText("Done").assertIsEnabled()
        (1..4).forEach { compose.onNodeWithText("Playlist $it").performScrollTo().performClick() }
        compose.runOnIdle { assertEquals(emptySet<String>(), saved); assertEquals(null, played) }
        compose.onNodeWithText("Playlist 5").assertIsNotEnabled()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Choose up to 4 playlists (4/4)").assertDoesNotExist()
        compose.onNodeWithContentDescription("Shuffle play Playlist 1").performClick()
        compose.runOnIdle { assertEquals("1", played); assertEquals(setOf("1", "2", "3", "4"), saved) }
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Playlist 1").performScrollTo().performClick()
        compose.onNodeWithText("Done").assertIsEnabled()
        compose.onNodeWithText("Playlist 5").performScrollTo().performClick()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithContentDescription("Shuffle play Playlist 1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Shuffle play Playlist 5").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(setOf("2", "3", "4", "5"), saved) }
    }

    @Test fun closingEditDiscardsDraftAndSavedBubblesReopen() {
        val saved = setOf("1", "2", "3")
        compose.setContent {
            RideWaveTheme { PlaylistButton(playlists, false, true, saved, {}, {}, {}, {}) }
        }
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Playlist 1").performScrollTo().performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.onNodeWithContentDescription("Shuffle play Playlist 1").assertIsDisplayed()
        compose.onNodeWithText("Done").assertDoesNotExist()
    }

    @Test fun existingFourSelectionsCanBeSaved() {
        compose.setContent {
            RideWaveTheme {
                PlaylistButton(playlists, false, true, setOf("1", "2", "3", "4"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.onNodeWithContentDescription("Shuffle play Playlist 4").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Done").assertIsEnabled()
    }

    @Test fun zeroAndTwoSelectionsCanBeSaved() {
        var saved: Set<String>? = null
        var played: String? = null
        compose.setContent {
            var ids by remember { mutableStateOf(emptySet<String>()) }
            var complete by remember { mutableStateOf(false) }
            RideWaveTheme {
                PlaylistButton(playlists, false, true, ids, {}, {},
                    { ids = it; saved = it; complete = true }, { played = it.uri },
                    setupComplete = complete)
            }
        }
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(emptySet<String>(), saved) }
        compose.onNodeWithText("No playlists selected. Tap Edit to add up to four.").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.onNodeWithText("Done").assertDoesNotExist()
        compose.onNodeWithText("Edit").performClick()
        (1..2).forEach { compose.onNodeWithText("Playlist $it").performScrollTo().performClick() }
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(setOf("1", "2"), saved) }
        compose.onNodeWithContentDescription("Shuffle play Playlist 2").performScrollTo().assertIsDisplayed()
    }

    @Test fun interactionRestartsFiveSecondTimeout() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RideWaveTheme {
                PlaylistButton(playlists, false, true, setOf("1"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Playlists").performClick()
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithContentDescription("Shuffle play Playlist 1").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText("PLAYLISTS").assertExists()
        compose.mainClock.advanceTimeBy(2_100)
        compose.onNodeWithText("PLAYLISTS").assertDoesNotExist()
    }
}
