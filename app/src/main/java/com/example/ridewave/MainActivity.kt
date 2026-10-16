package com.example.ridewave

import android.content.Context
import android.content.Intent
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
import com.example.ridewave.spotify.SpotifyWebApi
import com.example.ridewave.ui.theme.RideWaveTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var spotifyManager: SpotifyManager
    private lateinit var spotifyWebApi: SpotifyWebApi
    private var playlists by mutableStateOf(emptyList<SpotifyWebApi.Playlist>())
    private var playlistsLoading by mutableStateOf(false)
    private var playlistsAuthorized by mutableStateOf(false)
    private var selectedPlaylistIds by mutableStateOf(emptySet<String>())
    private var playlistSetupComplete by mutableStateOf(false)
    private var playlistError by mutableStateOf<String?>(null)

    private fun loadPlaylists(code: String? = null) {
        if (playlistsLoading) return
        playlistsLoading = true
        playlistError = null
        Thread {
            try {
                if (code != null) spotifyWebApi.exchangeAuthorizationCode(code)
                val result = spotifyWebApi.getPlaylists().distinctBy { it.id }
                runOnUiThread {
                    playlists = result
                    playlistsAuthorized = true
                    playlistsLoading = false
                }
            } catch (_: Exception) {
                runOnUiThread {
                    playlistsAuthorized = spotifyWebApi.getStoredToken() != null
                    playlistsLoading = false
                    playlistError = "Could not load Spotify playlists. Check your connection and retry."
                }
            }
        }.start()
    }

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

    private fun handleWebCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "ridewave" || uri.host != "callback") return
        val code = uri.getQueryParameter("code") ?: return
        loadPlaylists(code)
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
        spotifyWebApi = SpotifyWebApi(this)
        playlistsAuthorized = spotifyWebApi.getStoredToken() != null
        selectedPlaylistIds = spotifyPreferences.getStringSet("selected_playlist_ids", emptySet()).orEmpty()
        playlistSetupComplete = spotifyPreferences.getBoolean("playlist_setup_complete", selectedPlaylistIds.isNotEmpty())
        playlists = runCatching {
            val saved = JSONArray(spotifyPreferences.getString("selected_playlists", "[]"))
            List(saved.length()) { index ->
                val item = saved.getJSONObject(index)
                SpotifyWebApi.Playlist(item.getString("id"), item.getString("name"), item.getString("uri"),
                    item.optString("imageUrl").takeIf { it.startsWith("https://") })
            }
        }.getOrDefault(emptyList())
        handleWebCallback(intent)

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
                // These screens are onboarding states, not a navigable back stack. Once the
                // user advances, Back should leave RideWave instead of reopening completed setup.
                androidx.activity.compose.BackHandler { finish() }
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
                    playing = playing,
                    playlists = playlists, playlistsLoading = playlistsLoading,
                    selectedPlaylistIds = selectedPlaylistIds,
                    playlistSetupComplete = playlistSetupComplete,
                    playlistsAuthorized = playlistsAuthorized,
                    playlistError = playlistError,
                    onAuthorizePlaylists = {
                        startActivity(Intent(Intent.ACTION_VIEW, spotifyWebApi.createAuthorizationRequest().authorizationUri))
                    },
                    onLoadPlaylists = { loadPlaylists() },
                    onSavePlaylists = { ids ->
                        val chosen = playlists.filter { it.id in ids }
                        if (ids.size <= 4 && chosen.size == ids.size) {
                            val saved = JSONArray()
                            chosen.forEach {
                                saved.put(JSONObject().put("id", it.id).put("name", it.name).put("uri", it.uri)
                                    .put("imageUrl", it.imageUrl))
                            }
                            selectedPlaylistIds = ids.toSet()
                            playlistSetupComplete = true
                            spotifyPreferences.edit()
                                .putStringSet("selected_playlist_ids", ids)
                                .putBoolean("playlist_setup_complete", true)
                                .putString("selected_playlists", saved.toString()).apply()
                        }
                    },
                    onSelectPlaylist = {
                        playlistError = null
                        spotifyManager.shufflePlay(it.uri) { message ->
                            runOnUiThread { playlistError = message }
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWebCallback(intent)
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
