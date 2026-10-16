package com.example.ridewave.ui

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ridewave.spotify.SpotifyWebApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
internal fun PlaylistBubble(
    playlist: SpotifyWebApi.Playlist,
    diameter: Dp,
    onSelect: (SpotifyWebApi.Playlist) -> Unit
) {
    val artwork by produceState<ImageBitmap?>(null, playlist.imageUrl) {
        value = null
        value = withContext(Dispatchers.IO) {
            playlist.imageUrl?.let { loadPlaylistArtwork(it) }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(diameter)) {
        Surface(
            onClick = { onSelect(playlist) },
            shape = CircleShape,
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            modifier = Modifier.size(diameter).semantics {
                contentDescription = "Shuffle play ${playlist.name}"
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                val image = artwork
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(playlist.name, color = Color.White, textAlign = TextAlign.Center,
                        maxLines = 3, modifier = Modifier.padding(16.dp))
                }
            }
        }
        Text(playlist.name, color = Color.White, textAlign = TextAlign.Center,
            maxLines = 2, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Small bounded image fetch; failures retain the readable name fallback. */
internal fun loadPlaylistArtwork(url: String): ImageBitmap? {
    if (!url.startsWith("https://")) return null
    return runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val bytes = connection.inputStream.use { it.readBytesUpTo(2 * 1024 * 1024) }
                ?: return@runCatching null
            decodePlaylistArtwork(bytes)
                ?: error("Unsupported artwork format")
        } finally {
            connection.disconnect()
        }
    }.onFailure { Log.w("PlaylistArtwork", "Artwork load failed: ${it.javaClass.simpleName}: ${it.message}") }.getOrNull()
}

internal fun decodePlaylistArtwork(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val options = BitmapFactory.Options().apply { inSampleSize = 1 }
    while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 512) {
        options.inSampleSize *= 2
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

private fun java.io.InputStream.readBytesUpTo(limit: Int): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (output.size() + count > limit) return null
        output.write(buffer, 0, count)
    }
}
