package com.example.ridewave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.offset
import android.content.res.Configuration
import com.example.ridewave.R
import com.example.ridewave.gestures.GestureDetector
import com.example.ridewave.gestures.GestureType
import com.example.ridewave.ui.theme.RideWaveTheme

enum class RidePage { LANDING, CONNECT, RIDE }

private val Secondary = Color(0xFF999999)

@Composable
fun RideWaveScreen(
    page: RidePage,
    time: String,
    connected: Boolean,
    connecting: Boolean,
    error: String?,
    onContinue: () -> Unit,
    onConnect: () -> Unit,
    onRide: () -> Unit,
    onGesture: (GestureType) -> Unit,
    gestureDetector: GestureDetector,
    onClose: () -> Unit,
    feedback: String? = null,
    feedbackSequence: Int = 0,
    trackTitle: String? = null,
    trackArtist: String = "",
    playing: Boolean = false
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (page == RidePage.RIDE) {
            // A sibling close button sits above this pad and consumes its own taps.
            BoxWithConstraints(
                gestureDetector.createGestureModifier(
                    onSwipeLeft = { onGesture(GestureType.SWIPE_LEFT) },
                    onSwipeRight = { onGesture(GestureType.SWIPE_RIGHT) },
                    onTap = { onGesture(GestureType.SINGLE_TAP) }
                ).fillMaxSize().semantics {
                    contentDescription = "Ride controls"
                    customActions = listOf(
                        CustomAccessibilityAction("Play or pause") { onGesture(GestureType.SINGLE_TAP); true },
                        CustomAccessibilityAction("Next track") { onGesture(GestureType.SWIPE_LEFT); true },
                        CustomAccessibilityAction("Previous track") { onGesture(GestureType.SWIPE_RIGHT); true }
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                val landscape = maxWidth > maxHeight
                val clockSize = if (landscape) 80.sp else if (maxWidth >= 380.dp) 64.sp else 52.sp
                Text(time, color = Color.White, fontSize = clockSize, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif, letterSpacing = (-1).sp,
                    maxLines = 1, modifier = Modifier.align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                        .padding(horizontal = 88.dp, vertical = 8.dp))
                if (feedback != null) {
                    androidx.compose.runtime.key(feedbackSequence) {
                        val icon = when (feedback) {
                            "Play" -> R.drawable.ic_play
                            "Pause" -> R.drawable.ic_pause
                            "Next track" -> R.drawable.ic_next
                            "Previous track" -> R.drawable.ic_prev
                            else -> null
                        }
                        if (icon != null) {
                            Icon(painterResource(icon), contentDescription = feedback,
                                tint = Color.White, modifier = Modifier.size(if (landscape) 80.dp else 112.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite })
                        } else {
                            Text(feedback, color = Color.White, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp))
                        }
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                        .padding(start = 88.dp, end = 88.dp, top = if (landscape) 12.dp else 36.dp,
                            bottom = if (landscape) 12.dp else 36.dp)
                        .widthIn(max = 600.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (!connected) "CONNECTING TO SPOTIFY" else if (playing) "NOW PLAYING" else "PAUSED",
                        color = Color(0xFFBBBBBB), fontSize = 11.sp,
                        letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Text(if (connected) trackTitle ?: "Your next soundtrack" else "Getting your music ready",
                        color = Color.White, fontSize = if (landscape) 24.sp else 30.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        maxLines = if (landscape) 1 else 2, overflow = TextOverflow.Ellipsis)
                    Text(if (connected) trackArtist.ifEmpty { "Choose a song in Spotify" } else "Please wait",
                        color = Color(0xFFCCCCCC), fontSize = 18.sp, textAlign = TextAlign.Center,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(32.dp).height(2.dp).background(Color.White))
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 88.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("RIDEWAVE", color = Secondary, fontSize = 12.sp, letterSpacing = 3.sp)
                    if (page == RidePage.LANDING) {
                        Text("Your music.\nA simple gesture.", color = Color.White,
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
                        Text("Control Spotify with a tap or swipe. Ride mode keeps only your clock on a black screen.",
                            color = Secondary, style = MaterialTheme.typography.bodyLarge)
                        GestureRow("Tap anywhere", "Play or pause")
                        GestureRow("Swipe left", "Next track")
                        GestureRow("Swipe right", "Previous track")
                        Text("Connect before setting off.", color = Secondary,
                            style = MaterialTheme.typography.bodySmall)
                        ActionButton("Continue", true, onContinue)
                    } else {
                        Text(if (connected) "You're connected." else "Connect your soundtrack.",
                            color = Color.White, style = MaterialTheme.typography.headlineLarge)
                        Text(if (connected) "Spotify is ready. Start ride mode for fullscreen gesture controls."
                            else "Open Spotify and sign in on this device, then connect your account.",
                            color = Secondary, style = MaterialTheme.typography.bodyLarge)
                        if (connecting) {
                            CircularProgressIndicator(color = Secondary, modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp)
                            Text("Connecting to Spotify…", color = Secondary)
                        }
                        error?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
                        ActionButton(if (connected) "Start ride" else "Connect Spotify", !connecting,
                            if (connected) onRide else onConnect)
                    }
                }
            }
        }
        if (page == RidePage.RIDE) {
            ContactsButton(Modifier.align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp))
        }
        NotificationButton(Modifier.align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp))
        IconButton(onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp).size(72.dp)
                .offset(x = if (landscape) (-40).dp else 0.dp)
                .semantics { contentDescription = "Close RideWave" }) {
            Text("×", color = Color.White, fontSize = 44.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GestureRow(gesture: String, action: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(gesture, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(action, color = Secondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White,
            disabledContentColor = Secondary)) {
        Text(label)
    }
}

@Preview(name = "Ride portrait", widthDp = 393, heightDp = 852)
@Preview(name = "Ride landscape", widthDp = 852, heightDp = 393)
@Composable
private fun RidePreview() {
    RideWaveTheme {
        RideWaveScreen(RidePage.RIDE, "09:41", true, false, null, {}, {}, {}, {}, GestureDetector(), {})
    }
}

@Preview(name = "Introduction", widthDp = 393, heightDp = 852)
@Composable
private fun LandingPreview() {
    RideWaveTheme {
        RideWaveScreen(RidePage.LANDING, "", false, false, null, {}, {}, {}, {}, GestureDetector(), {})
    }
}
