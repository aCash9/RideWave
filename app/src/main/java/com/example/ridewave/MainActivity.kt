package com.example.ridewave

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.ridewave.spotify.SpotifyManager
import com.example.ridewave.ui.theme.RideWaveTheme

import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var spotifyManager: SpotifyManager

    private var isAuthorized by mutableStateOf(false)

    private var isConnected by mutableStateOf(false)

    private var isConnecting by mutableStateOf(false)

    private var errorMessage by mutableStateOf<String?>(null)

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

                    errorMessage =
                        "Spotify authorization did not complete."
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        spotifyManager =
            SpotifyManager(this)

        setContent {

            RideWaveTheme {

                Scaffold(
                    modifier =
                        Modifier.fillMaxSize()
                ) { innerPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),

                        verticalArrangement =
                            Arrangement.Center,

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "RideWave",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineLarge
                        )

                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
                        )

                        when {

                            isConnected -> {

                                SpotifyControls()
                            }

                            else -> {

                                SpotifyLoginButton()
                            }
                        }

                        errorMessage?.let { error ->

                            Spacer(
                                modifier =
                                    Modifier.height(24.dp)
                            )

                            Text(
                                text = error,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )
                        }
                    }
                }
            }
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
            }
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

        Text(
            text = "Spotify Connected!",
            style =
                MaterialTheme
                    .typography
                    .titleLarge
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {
                spotifyManager.play()
            }
        ) {
            Text("Play")
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                spotifyManager.pause()
            }
        ) {
            Text("Pause")
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                spotifyManager.previous()
            }
        ) {
            Text("Previous")
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                spotifyManager.next()
            }
        ) {
            Text("Next")
        }
    }

    override fun onStop() {

        super.onStop()

        /*
         * Do NOT disconnect while Spotify's
         * authorization Activity is being shown.
         *
         * We'll handle connection lifecycle more
         * carefully once basic authorization works.
         */
    }

    override fun onDestroy() {

        spotifyManager.disconnect()

        super.onDestroy()
    }
}