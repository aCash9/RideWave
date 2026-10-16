package com.example.ridewave.gestures

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Enum representing the different gesture types
 */
enum class GestureType {
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SINGLE_TAP
}

/**
 * Gesture detector class that provides gesture detection logic
 */
class GestureDetector {

    companion object {
        private const val SWIPE_THRESHOLD = 100f
    }

    /**
     * Creates a modifier that detects gestures and invokes callbacks
     *
     * @param onSwipeLeft Callback for left swipe
     * @param onSwipeRight Callback for right swipe
     * @param onTap Callback for single tap
     * @return Modifier that can be applied to any Composable
     */
    fun createGestureModifier(
        onSwipeLeft: () -> Unit = {},
        onSwipeRight: () -> Unit = {},
        onTap: () -> Unit = {}
    ): Modifier {
        return Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    onTap()
                }
            )
        }.pointerInput(Unit) {
            var totalDragX = 0f
            var totalDragY = 0f

            detectDragGestures(
                onDragStart = {
                    totalDragX = 0f
                    totalDragY = 0f
                },
                onDragEnd = {
                    if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)) {
                        if (kotlin.math.abs(totalDragX) > SWIPE_THRESHOLD) {
                            if (totalDragX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    totalDragX += dragAmount.x
                    totalDragY += dragAmount.y
                }
            )
        }
    }
}
