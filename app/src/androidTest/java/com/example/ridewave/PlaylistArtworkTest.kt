package com.example.ridewave

import android.graphics.Bitmap
import com.example.ridewave.ui.decodePlaylistArtwork
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class PlaylistArtworkTest {
    @Test fun decodesSmallCoverWithoutZeroSampleSize() {
        val image = decodePlaylistArtwork(png(32))
        assertNotNull(image)
        assertEquals(32, image!!.width)
    }

    @Test fun downsamplesLargeCovers() {
        val image = decodePlaylistArtwork(png(1024))
        assertNotNull(image)
        assertTrue(image!!.width <= 512)
    }

    @Test fun invalidCoverReturnsNull() {
        assertNull(decodePlaylistArtwork(byteArrayOf(1, 2, 3)))
    }

    private fun png(size: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        return try {
            ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
