package com.example.ridewave.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.AtomicFile
import android.util.Base64
import com.google.gson.Gson
import java.io.File

internal data class RideContact(val name: String, val number: String, val photo: String? = null)

internal class RideContacts(private val context: Context) {
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
            arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.PHOTO_THUMBNAIL_URI),
            null, null, null)?.use { cursor ->
            check(cursor.moveToFirst()) { "Contact unavailable" }
            val number = cursor.getString(1).orEmpty()
            check(number.isNotBlank()) { "No phone number" }
            val photoUri = cursor.getString(2)
            val photo = try {
                photoUri?.let { value ->
                    context.contentResolver.openInputStream(Uri.parse(value))?.use { input ->
                        // Thumbnails are optional; bound the size of locally retained data.
                        val bytes = input.readBytesLimited(256 * 1024)
                        bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    }
                }
            } catch (_: Exception) { null }
            RideContact(cursor.getString(0).orEmpty().ifBlank { number }, number, photo)
        } ?: error("Contact unavailable")
    }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (output.size() + count > limit) return null
        output.write(buffer, 0, count)
    }
}
