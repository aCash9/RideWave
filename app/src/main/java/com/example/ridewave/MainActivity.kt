package com.example.ridewave

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.util.Log
import com.example.ridewave.R

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image

import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    private var isAuthorized by mutableStateOf(false)

    private var isConnected by mutableStateOf(false)

    private var isConnecting by mutableStateOf(false)

    private var isAuthorizing = false

    private var errorMessage by mutableStateOf<String?>(null)

    private var currentTime by mutableStateOf("")

    private var gestureFeedback by mutableStateOf<GestureType?>(null)

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

                    isAuthorized = true
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

                    isAuthorized = false
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

                    isAuthorized = false
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

                    isAuthorized = false
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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        spotifyManager =
            SpotifyManager(this)

        gestureDetector = GestureDetector()

        gestureHandler = GestureHandler(spotifyManager) { gesture ->
            gestureFeedback = gesture
        }

        setContent {

            RideWaveTheme {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                ) {
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date())
                            kotlinx.coroutines.delay(1000)
                        }
                    }

                    LaunchedEffect(gestureFeedback) {
                        if (gestureFeedback != null) {
                            kotlinx.coroutines.delay(1500)
                            gestureFeedback = null
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(24.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        finish()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 24.dp),

                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = currentTime,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 80.sp
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            when {
                                isConnected -> {
                                    SpotifyControls()
                                }
                                else -> {
                                    SpotifyLoginButton()
                                }
                            }

                            errorMessage?.let { error ->
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = error,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = gestureFeedback != null,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        gestureFeedback?.let { gesture ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(
                                        id = when (gesture) {
                                            GestureType.SWIPE_LEFT -> R.drawable.ic_next
                                            GestureType.SWIPE_RIGHT -> R.drawable.ic_prev
                                            GestureType.SINGLE_TAP -> R.drawable.ic_play
                                        }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }
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

    @Composable
    private fun SpotifyLoginButton() {

        Button(
            enabled = !isConnecting,
            onClick = {

                errorMessage = null

                if (!spotifyManager.isSpotifyInstalled()) {

                    errorMessage =
                        "Spotify is not installed."

                    return@Button
                }

                isConnecting = true
                isAuthorizing = true

                Log.d(
                    TAG,
                    "Launching Spotify authorization"
                )

                val authIntent =
                    spotifyManager.getAuthIntent(
                        this@MainActivity
                    )

                authLauncher.launch(
                    authIntent
                )
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White.copy(alpha = 0.6f)
            )
        ) {

            Text(
                text =
                    if (isConnecting) {
                        "Connecting..."
                    } else {
                        "Connect Spotify"
                    }
            )
        }
    }

    override fun onStart() {
        super.onStart()

        val previouslyAuthorized =
            spotifyPreferences.getBoolean("spotify_authorized", false)

        if (previouslyAuthorized && !isConnected && !isConnecting) {
            connectSpotifyAutomatically()
        }
    }

    private fun connectSpotifyAutomatically() {

        if (!spotifyManager.isSpotifyInstalled()) {
            errorMessage = "Spotify is not installed"
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

    @Composable
    private fun SpotifyControls() {

        Box(
            modifier = gestureDetector.createGestureModifier(
                onSwipeLeft = { gestureHandler.handleGesture(GestureType.SWIPE_LEFT) },
                onSwipeRight = { gestureHandler.handleGesture(GestureType.SWIPE_RIGHT) },
                onTap = { gestureHandler.handleGesture(GestureType.SINGLE_TAP) }
            ).fillMaxSize()
        ) {
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
        }
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}