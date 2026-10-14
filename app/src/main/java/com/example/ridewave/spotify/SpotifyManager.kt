package com.example.ridewave.spotify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "SpotifyManager"
        private const val CONNECTION_TIMEOUT_MS = 15_000L
    }

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private var connecting = false

    private val handler = Handler(
        Looper.getMainLooper()
    )

    /**
     * Check whether Spotify is installed.
     */
    fun isSpotifyInstalled(): Boolean {
        return try {

            context.packageManager.getPackageInfo(
                "com.spotify.music",
                0
            )

            Log.d(TAG, "Spotify app FOUND")

            true

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Spotify app NOT FOUND",
                exception
            )

            false
        }
    }

    /**
     * Creates the explicit Spotify authorization intent.
     *
     * We request app-remote-control because RideWave
     * needs to control Spotify playback.
     */
    fun getAuthIntent(
        activity: Activity
    ): Intent {

        Log.d(TAG, "Creating Spotify authorization request")

        val request = AuthorizationRequest.Builder(
            SpotifyConfig.CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            SpotifyConfig.REDIRECT_URI
        )
            .setScopes(SpotifyConfig.SCOPES)
            .build()

        return AuthorizationClient.createLoginActivityIntent(
            activity,
            request
        )
    }

    /**
     * Connect to Spotify App Remote AFTER explicit
     * authorization has completed successfully.
     */
    fun connect(
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit
    ) {

        if (!isSpotifyInstalled()) {

            onError(
                IllegalStateException(
                    "Spotify is not installed on this device."
                )
            )

            return
        }

        if (spotifyAppRemote?.isConnected == true) {

            Log.d(TAG, "Spotify already connected")

            onConnected()

            return
        }

        if (connecting) {

            Log.d(
                TAG,
                "Spotify connection already in progress"
            )

            return
        }

        connecting = true

        Log.d(
            TAG,
            "Starting Spotify App Remote connection"
        )

        val connectionParams =
            ConnectionParams.Builder(
                SpotifyConfig.CLIENT_ID
            )
                .setRedirectUri(
                    SpotifyConfig.REDIRECT_URI
                )

                /*
                 * Authorization has already happened
                 * through AuthorizationClient.
                 */
                .showAuthView(false)

                .build()

        val timeoutRunnable = Runnable {

            if (connecting) {

                connecting = false

                Log.e(
                    TAG,
                    "Spotify connection timed out"
                )

                onError(
                    Exception(
                        "Spotify connection timed out after 15 seconds"
                    )
                )
            }
        }

        handler.postDelayed(
            timeoutRunnable,
            CONNECTION_TIMEOUT_MS
        )

        SpotifyAppRemote.connect(
            context,
            connectionParams,
            object : Connector.ConnectionListener {

                override fun onConnected(
                    appRemote: SpotifyAppRemote
                ) {

                    handler.removeCallbacks(
                        timeoutRunnable
                    )

                    connecting = false

                    spotifyAppRemote = appRemote

                    Log.d(
                        TAG,
                        "Spotify App Remote connected successfully"
                    )

                    onConnected()
                }

                override fun onFailure(
                    throwable: Throwable
                ) {

                    handler.removeCallbacks(
                        timeoutRunnable
                    )

                    connecting = false

                    spotifyAppRemote = null

                    Log.e(
                        TAG,
                        "Spotify App Remote connection failed",
                        throwable
                    )

                    onError(throwable)
                }
            }
        )
    }

    fun isConnected(): Boolean {
        return spotifyAppRemote?.isConnected == true
    }

    fun play() {

        val remote = spotifyAppRemote

        if (remote == null) {
            Log.e(TAG, "Cannot play: Spotify not connected")
            return
        }

        remote.playerApi
            .resume()
            .setResultCallback {
                Log.d(TAG, "Playback resumed")
            }
            .setErrorCallback { error ->
                Log.e(
                    TAG,
                    "Failed to resume playback",
                    error
                )
            }
    }

    fun pause() {

        val remote = spotifyAppRemote

        if (remote == null) {
            Log.e(TAG, "Cannot pause: Spotify not connected")
            return
        }

        remote.playerApi
            .pause()
            .setResultCallback {
                Log.d(TAG, "Playback paused")
            }
            .setErrorCallback { error ->
                Log.e(
                    TAG,
                    "Failed to pause playback",
                    error
                )
            }
    }

    fun next() {

        val remote = spotifyAppRemote

        if (remote == null) {
            Log.e(TAG, "Cannot skip: Spotify not connected")
            return
        }

        remote.playerApi
            .skipNext()
            .setResultCallback {
                Log.d(TAG, "Skipped to next track")
            }
            .setErrorCallback { error ->
                Log.e(
                    TAG,
                    "Failed to skip next",
                    error
                )
            }
    }

    fun previous() {

        val remote = spotifyAppRemote

        if (remote == null) {
            Log.e(TAG, "Cannot skip: Spotify not connected")
            return
        }

        remote.playerApi
            .skipPrevious()
            .setResultCallback {
                Log.d(TAG, "Skipped to previous track")
            }
            .setErrorCallback { error ->
                Log.e(
                    TAG,
                    "Failed to skip previous",
                    error
                )
            }
    }

    fun disconnect() {

        Log.d(TAG, "Disconnecting Spotify")

        connecting = false

        spotifyAppRemote?.let { remote ->

            SpotifyAppRemote.disconnect(
                remote
            )
        }

        spotifyAppRemote = null
    }
}