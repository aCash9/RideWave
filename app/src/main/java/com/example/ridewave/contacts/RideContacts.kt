package com.example.ridewave.contacts

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.AtomicFile
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

internal data class RideContact(val name: String, val number: String, val photo: String? = null)

internal class RideContacts(private val context: Context) {
    companion object {
        private const val TAG = "RideContacts"
        private const val MAX_SOURCE_PHOTO_BYTES = 8 * 1024 * 1024
    }

    private val file = AtomicFile(File(context.noBackupFilesDir, "ride_contacts.json"))
    private val gson = Gson()

    fun load(): List<RideContact> = if (!file.baseFile.exists()) emptyList() else
        file.openRead().bufferedReader().use {
            gson.fromJson(it, Array<RideContact>::class.java).toList().take(4)
        }

    fun save(contacts: List<RideContact>) {
        val stream = file.startWrite()
        try {
            stream.write(gson.toJson(contacts.take(4)).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (exception: Exception) {
            file.failWrite(stream)
            throw exception
        }
    }

    // The picker grants access to this selected phone row, not the address book.
    fun readSelection(uri: Uri): RideContact {
        return context.contentResolver.query(uri,
            arrayOf(
                Phone.DISPLAY_NAME,
                Phone.NUMBER,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.PHOTO_URI,
                Phone.CONTACT_ID
            ),
            null, null, null)?.use { cursor ->
            check(cursor.moveToFirst()) { "Contact unavailable" }
            val name = cursor.string(Phone.DISPLAY_NAME)
            val number = cursor.string(Phone.NUMBER)
            check(number.isNotBlank()) { "No phone number" }
            val photo = readPhoto(
                listOfNotNull(
                    cursor.string(Phone.PHOTO_THUMBNAIL_URI).takeIf(String::isNotBlank),
                    cursor.string(Phone.PHOTO_URI).takeIf(String::isNotBlank)
                ),
                cursor.long(Phone.CONTACT_ID)
            )
            RideContact(name.ifBlank { number }, number, photo)
        } ?: error("Contact unavailable")
    }

    private fun readPhoto(photoUris: List<String>, contactId: Long?): String? {
        photoUris.distinct().forEach { value ->
            encodePhoto {
                context.contentResolver.openInputStream(Uri.parse(value))
            }?.let { return it }
        }
        if (contactId != null && contactId > 0) {
            val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
            encodePhoto {
                Contacts.openContactPhotoInputStream(context.contentResolver, contactUri, true)
            }?.let { return it }
        }
        return null
    }

    private fun encodePhoto(openStream: () -> InputStream?): String? = try {
        openStream()?.use { input ->
            val source = input.readBytesLimited(MAX_SOURCE_PHOTO_BYTES) ?: return null
            resizeContactPhoto(source)?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        }
    } catch (error: Exception) {
        Log.w(TAG, "Selected contact photo could not be read: ${error.javaClass.simpleName}")
        null
    }
}

private fun android.database.Cursor.string(column: String): String =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getString).orEmpty()

private fun android.database.Cursor.long(column: String): Long? =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong)

internal fun resizeContactPhoto(source: ByteArray): ByteArray? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = 1 }
    while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > MAX_CONTACT_PHOTO_EDGE * 2) {
        options.inSampleSize *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, options) ?: return null
    val scale = minOf(1f, MAX_CONTACT_PHOTO_EDGE.toFloat() / maxOf(decoded.width, decoded.height))
    val resized = if (scale < 1f) Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1),
        true
    ) else decoded
    return ByteArrayOutputStream().use { output ->
        resized.compress(Bitmap.CompressFormat.JPEG, 88, output)
        if (resized !== decoded) resized.recycle()
        decoded.recycle()
        output.toByteArray()
    }
}

private const val MAX_CONTACT_PHOTO_EDGE = 512

private fun InputStream.readBytesLimited(limit: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (output.size() + count > limit) return null
        output.write(buffer, 0, count)
    }
}
