package com.example.ridewave

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ridewave.contacts.RideContact
import com.example.ridewave.contacts.RideContacts
import com.example.ridewave.contacts.resizeContactPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class RideContactsTest {
    @Test fun contactsPersistAndAreCappedAtFour() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.cacheDir, "contacts-test-${System.nanoTime()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getNoBackupFilesDir() = directory
        }
        try {
            val store = RideContacts(context)
            assertEquals(emptyList<RideContact>(), store.load())
            val contacts = (1..5).map { RideContact("Person $it", "555000$it") }
            store.save(contacts)
            assertEquals(contacts.take(4), RideContacts(context).load())
            store.save(emptyList())
            assertEquals(emptyList<RideContact>(), store.load())
        } finally { directory.deleteRecursively() }
    }

    @Test fun contactPhotosAreNormalizedAndBoundedForBubbleStorage() {
        val source = ByteArrayOutputStream().use { output ->
            Bitmap.createBitmap(1400, 900, Bitmap.Config.ARGB_8888).run {
                eraseColor(android.graphics.Color.BLUE)
                compress(Bitmap.CompressFormat.PNG, 100, output)
                recycle()
            }
            output.toByteArray()
        }

        val normalized = resizeContactPhoto(source)
        val bitmap = BitmapFactory.decodeByteArray(normalized, 0, normalized!!.size)
        assertTrue(maxOf(bitmap.width, bitmap.height) <= 512)
        assertTrue(normalized.isNotEmpty())
        bitmap.recycle()
        assertNull(resizeContactPhoto(byteArrayOf(1, 2, 3)))
    }
}
