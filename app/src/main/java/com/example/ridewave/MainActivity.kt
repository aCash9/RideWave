package com.example.ridewave

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ridewave.gestures.GestureDetector
import com.example.ridewave.gestures.GestureHandler
import com.example.ridewave.gestures.GestureType
import com.example.ridewave.spotify.SpotifyManager
import com.example.ridewave.ui.theme.RideWaveTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var spotifyManager: SpotifyManager

    private lateinit var gestureDetector: GestureDetector

    private lateinit var gestureHandler: GestureHandler

    private var page by mutableStateOf(com.example.ridewave.ui.RidePage.LANDING)

    private var isConnected by mutableStateOf(false)

    private var isConnecting by mutableStateOf(false)

    private var isAuthorizing = false

    private var errorMessage by mutableStateOf<String?>(null)

    private var currentTime by mutableStateOf("")

    private var feedback by mutableStateOf<String?>(null)
    private var feedbackSequence by mutableStateOf(0)
    private var trackTitle by mutableStateOf<String?>(null)
    private var trackArtist by mutableStateOf("")
    private var playing by mutableStateOf(false)



    private val spotifyPreferences by lazy {
        getSharedPreferences("spotify_preferences", Context.MODE_PRIVATE)
    }

    /*
     * Handles the result returned from Spotify's
     * authorization screen.
     */
    private val authLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val response =
                AuthorizationClient.getResponse(
                    result.resultCode,
                    result.data
                )

            Log.d(
                TAG,
                "Spotify auth response: ${response.type}"
            )

            when (response.type) {

                AuthorizationResponse.Type.TOKEN -> {

                    Log.d(
                        TAG,
                        "Spotify authorization SUCCESS"
                    )

                    isAuthorizing = false

                    spotifyPreferences.edit()
                        .putBoolean("spotify_authorized", true)
                        .apply()

                    /*
                     * Authorization worked.
                     *
                     * Now connect App Remote.
                     */
                    connectSpotifyRemote()
                }

                AuthorizationResponse.Type.ERROR -> {

                    Log.e(
                        TAG,
                        "Spotify authorization ERROR: ${response.error}"
                    )

                    isConnecting = false
                    isAuthorizing = false

                    errorMessage =
                        "Spotify authorization failed: ${response.error}"
                }

                AuthorizationResponse.Type.EMPTY -> {

                    Log.d(
                        TAG,
                        "Spotify authorization cancelled"
                    )

                    isConnecting = false
                    isAuthorizing = false

                    errorMessage =
                        "Spotify authorization was cancelled."
                }

                else -> {

                    Log.d(
                        TAG,
                        "Spotify authorization returned: ${response.type}"
                    )

                    isConnecting = false
                    isAuthorizing = false

                    errorMessage =
                        "Spotify authorization did not complete."
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        val authorized = spotifyPreferences.getBoolean("spotify_authorized", false)
        page = if (spotifyPreferences.getBoolean("onboarding_seen", authorized)) {
            if (authorized) com.example.ridewave.ui.RidePage.RIDE else com.example.ridewave.ui.RidePage.CONNECT
        } else com.example.ridewave.ui.RidePage.LANDING

        enableEdgeToEdge()

        spotifyManager =
            SpotifyManager(this)

        gestureDetector = GestureDetector()

        gestureHandler = GestureHandler(spotifyManager) { gesture ->
            feedback = when (gesture) {
                GestureType.SWIPE_LEFT -> "Next track"
                GestureType.SWIPE_RIGHT -> "Previous track"
                GestureType.SINGLE_TAP -> if (gestureHandler.isPlaying()) "Play" else "Pause"
            }
            feedbackSequence++
        }

        setContent {
            RideWaveTheme {
                LaunchedEffect(feedbackSequence) {
                    if (feedback != null) {
                        kotlinx.coroutines.delay(2000)
                        feedback = null
                    }
                }
                LaunchedEffect(page) {
                    if (page == com.example.ridewave.ui.RidePage.RIDE) {
                        while (true) {
                            currentTime = android.text.format.DateFormat.getTimeFormat(this@MainActivity)
                                .format(java.util.Date())
                            kotlinx.coroutines.delay(60_000L - System.currentTimeMillis() % 60_000L)
                        }
                    }
                }
                LaunchedEffect(page) {
                    if (page == com.example.ridewave.ui.RidePage.RIDE) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                androidx.activity.compose.BackHandler(page != com.example.ridewave.ui.RidePage.LANDING) {
                    page = if (page == com.example.ridewave.ui.RidePage.RIDE)
                        com.example.ridewave.ui.RidePage.CONNECT else com.example.ridewave.ui.RidePage.LANDING
                }
                com.example.ridewave.ui.RideWaveScreen(
                    page = page,
                    time = currentTime,
                    connected = isConnected,
                    connecting = isConnecting,
                    error = errorMessage,
                    onContinue = {
                        spotifyPreferences.edit().putBoolean("onboarding_seen", true).apply()
                        page = com.example.ridewave.ui.RidePage.CONNECT
                        if (spotifyPreferences.getBoolean("spotify_authorized", false) && !isConnected && !isConnecting) {
                            connectSpotifyAutomatically()
                        }
                    },
                    onConnect = ::authorizeSpotify,
                    onRide = { if (isConnected) page = com.example.ridewave.ui.RidePage.RIDE },
                    onGesture = {
                        if (isConnected) gestureHandler.handleGesture(it)
                        else {
                            feedback = if (isConnecting) "Connecting to Spotify…" else "Spotify is disconnected"
                            feedbackSequence++
                        }
                    },
                    gestureDetector = gestureDetector,
                    onClose = { finish() },
                    feedback = feedback,
                    feedbackSequence = feedbackSequence,
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                    playing = playing
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun authorizeSpotify() {
        if (isConnecting) return
        errorMessage = null
        if (!spotifyManager.isSpotifyInstalled()) {
            errorMessage = "Install Spotify and sign in, then try again."
            return
        }
        isConnecting = true
        isAuthorizing = true
        authLauncher.launch(spotifyManager.getAuthIntent(this))
    }

    override fun onStart() {
        super.onStart()

        val previouslyAuthorized =
            spotifyPreferences.getBoolean("spotify_authorized", false)

        if (page != com.example.ridewave.ui.RidePage.LANDING && previouslyAuthorized && !isConnected && !isConnecting) {
            connectSpotifyAutomatically()
        }
    }

    private fun connectSpotifyAutomatically() {

        if (!spotifyManager.isSpotifyInstalled()) {
            errorMessage = "Spotify is not installed"
            page = com.example.ridewave.ui.RidePage.CONNECT
            return
        }

        isConnecting = true
        errorMessage = null

        Log.d(TAG, "Attempting automatic Spotify connection")

        spotifyManager.connect(

            onConnected = {

                runOnUiThread {
                    isConnected = true
                    isConnecting = false
                    errorMessage = null

                    observePlayback()
                    page = com.example.ridewave.ui.RidePage.RIDE

                    Log.d(TAG, "Spotify auto-connected successfully")
                }
            },

            onError = { throwable ->

                runOnUiThread {
                    isConnected = false
                    isConnecting = false

                    Log.w(
                        TAG,
                        "Automatic connection failed",
                        throwable
                    )

                    errorMessage =
                        "Automatic connection failed. Tap Connect Spotify."
                    page = com.example.ridewave.ui.RidePage.CONNECT
                }
            }
        )
    }

    private fun connectSpotifyRemote() {

        Log.d(
            TAG,
            "Authorization complete. Connecting App Remote..."
        )

        spotifyManager.connect(

            onConnected = {

                runOnUiThread {

                    Log.d(
                        TAG,
                        "Spotify fully connected"
                    )

                    isConnected = true
                    isConnecting = false
                    errorMessage = null

                    observePlayback()
                    page = com.example.ridewave.ui.RidePage.RIDE
                }
            },

            onError = { throwable ->

                runOnUiThread {

                    Log.e(
                        TAG,
                        "Spotify App Remote connection failed",
                        throwable
                    )

                    isConnected = false
                    isConnecting = false

                    errorMessage =
                        throwable.message
                            ?: "Spotify connection failed."
                }
            }
        )
    }

    private fun observePlayback() {
        spotifyManager.observePlayback { state ->
            runOnUiThread {
                playing = !state.isPaused
                gestureHandler.setPlayingState(playing)
                trackTitle = state.track?.name
                trackArtist = state.track?.artist?.name.orEmpty()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onStop() {

        super.onStop()

        if (!isAuthorizing) {
            spotifyManager.disconnect()
            isConnected = false
            isConnecting = false
            feedback = null
        }
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}
