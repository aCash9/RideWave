package com.example.ridewave.spotify

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Minimal Spotify Web API client for Android's PKCE authorization flow.
 * Network methods must be called away from the main thread.
 */
class SpotifyWebApi(context: Context) {

    data class Playlist(val id: String, val name: String, val uri: String, val imageUrl: String? = null)

    data class Token(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAtMillis: Long
    )

    data class PkceRequest(
        val authorizationUri: Uri,
        val verifier: String
    )

    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun createAuthorizationRequest(): PkceRequest {
        val verifier = createVerifier()
        val challenge = createChallenge(verifier)
        val uri = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", SpotifyConfig.CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", SpotifyConfig.REDIRECT_URI)
            .appendQueryParameter("scope", SpotifyConfig.SCOPES.joinToString(" "))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()

        preferences.edit().putString(KEY_VERIFIER, verifier).apply()
        return PkceRequest(uri, verifier)
    }

    fun exchangeAuthorizationCode(code: String): Token {
        val verifier = preferences.getString(KEY_VERIFIER, null)
            ?: throw IllegalStateException("Missing PKCE verifier")

        val token = postForm(
            TOKEN_URL,
            mapOf(
                "client_id" to SpotifyConfig.CLIENT_ID,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to SpotifyConfig.REDIRECT_URI,
                "code_verifier" to verifier
            )
        )
        return saveToken(token, preferences.getString(KEY_REFRESH_TOKEN, null))
    }

    fun refreshAccessToken(): Token {
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)
            ?: throw IllegalStateException("No Spotify refresh token is stored")

        val token = postForm(
            TOKEN_URL,
            mapOf(
                "client_id" to SpotifyConfig.CLIENT_ID,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken
            )
        )
        return saveToken(token, refreshToken)
    }

    fun getStoredToken(): Token? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)
        return Token(accessToken, refreshToken, expiresAt)
    }

    fun getPlaylists(): List<Playlist> {
        val result = mutableListOf<Playlist>()
        var offset = 0
        while (true) {
            val page = apiGet("me/playlists?limit=50&offset=$offset")
            val items = page.optJSONArray("items") ?: break
            for (index in 0 until items.length()) {
                items.optJSONObject(index)?.let { item ->
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val uri = item.optString("uri")
                    val imageUrl = item.optJSONArray("images")?.optJSONObject(0)
                        ?.optString("url")?.takeIf { it.startsWith("https://") }
                    if (id.isNotEmpty() && name.isNotEmpty() && uri.isNotEmpty()) {
                        result += Playlist(id, name, uri, imageUrl)
                    }
                }
            }
            offset += items.length()
            if (page.isNull("next") || items.length() == 0) break
        }
        return result
    }

    private fun apiGet(path: String): JSONObject {
        val token = getStoredToken() ?: throw IllegalStateException("Spotify Web API is not authorized")
        val activeToken = if (token.expiresAtMillis <= System.currentTimeMillis() + TOKEN_EXPIRY_BUFFER_MILLIS) {
            refreshAccessToken()
        } else {
            token
        }

        val connection = URL("$API_BASE_URL/$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Authorization", "Bearer ${activeToken.accessToken}")
        return readJson(connection)
    }

    private fun postForm(url: String, values: Map<String, String>): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val body = values.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return readJson(connection)
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) throw IOException("Spotify Web API returned HTTP $status: $body")
        return JSONObject(body)
    }

    private fun saveToken(json: JSONObject, existingRefreshToken: String?): Token {
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token", existingRefreshToken ?: "")
            .takeIf { it.isNotEmpty() }
        val expiresAt = System.currentTimeMillis() + json.getLong("expires_in") * 1000L
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .remove(KEY_VERIFIER)
            .apply()
        return Token(accessToken, refreshToken, expiresAt)
    }

    private fun createVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun createChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun encode(value: String): String = Uri.encode(value)

    companion object {
        private const val PREFERENCES = "spotify_web_api"
        private const val KEY_VERIFIER = "pkce_verifier"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val TOKEN_EXPIRY_BUFFER_MILLIS = 60_000L
        private const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val API_BASE_URL = "https://api.spotify.com/v1"
    }
}
