package com.example.ridewave.gestures

import com.example.ridewave.spotify.SpotifyManager
import android.util.Log

/**
 * Gesture handler that maps gestures to Spotify actions
 */
class GestureHandler(
    private val spotifyManager: SpotifyManager,
    private val onGestureDetected: (GestureType) -> Unit = {}
) {

    companion object {
        private const val TAG = "GestureHandler"
    }

    private var isPlaying = false

    /**
     * Handles a gesture by mapping it to the appropriate Spotify action
     *
     * Gesture mapping:
     * - SWIPE_RIGHT -> previous track
     * - SWIPE_LEFT -> next track
     * - SINGLE_TAP -> toggle play/pause
     *
     * @param gesture The gesture type to handle
     */
    fun handleGesture(gesture: GestureType) {
        when (gesture) {
            GestureType.SWIPE_RIGHT -> {
                Log.d(TAG, "Swipe right detected -> Previous track")
                spotifyManager.previous()
                onGestureDetected(gesture)
            }
            GestureType.SWIPE_LEFT -> {
                Log.d(TAG, "Swipe left detected -> Next track")
                spotifyManager.next()
                onGestureDetected(gesture)
            }
            GestureType.SINGLE_TAP -> {
                if (isPlaying) {
                    Log.d(TAG, "Single tap detected -> Pause")
                    spotifyManager.pause()
                    isPlaying = false
                    onGestureDetected(gesture)
                } else {
                    Log.d(TAG, "Single tap detected -> Play")
                    spotifyManager.play()
                    isPlaying = true
                    onGestureDetected(gesture)
                }
            }
        }
    }

    fun setPlayingState(playing: Boolean) {
        isPlaying = playing
    }

    fun isPlaying(): Boolean {
        return isPlaying
    }
}
