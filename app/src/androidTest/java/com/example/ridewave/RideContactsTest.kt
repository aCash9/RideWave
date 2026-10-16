package com.example.ridewave

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ridewave.contacts.RideContact
import com.example.ridewave.contacts.RideContacts
import org.junit.Assert.assertEquals
import org.junit.Test
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
}
