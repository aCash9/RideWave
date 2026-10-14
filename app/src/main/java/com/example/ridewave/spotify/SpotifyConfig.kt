package com.example.ridewave.spotify

object SpotifyConfig {

    // Spotify Developer Dashboard -> Your App -> Client ID
    const val CLIENT_ID = "0b2b8e3756e746ce89a7a4a08796f4ea"

    // Must exactly match Spotify Developer Dashboard
    const val REDIRECT_URI = "ridewave://callback"

    val SCOPES = arrayOf(
        "app-remote-control"
    )
}