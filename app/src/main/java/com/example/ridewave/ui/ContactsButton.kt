package com.example.ridewave.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ridewave.R
import com.example.ridewave.contacts.RideContact
import com.example.ridewave.contacts.RideContacts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { RideContacts(context.applicationContext) }
    val contactsLoaded = remember(store) { CompletableDeferred<Unit>() }
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf(emptyList<RideContact>()) }
    var open by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var permissionPending by rememberSaveable { mutableStateOf(false) }
    var permissionHelp by rememberSaveable { mutableStateOf(false) }
    var permissionNeedsSettings by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var pendingCallNumber by rememberSaveable { mutableStateOf<String?>(null) }
    val activity = remember(context) { context.findActivity() }
    fun message(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    fun canCall() = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED
    fun dismiss() {
        open = false
        permissionHelp = false
        pendingCallNumber = null
    }
    fun call(number: String) {
        if (!open) return
        // Consume the tap before handing over to the phone app to prevent duplicate calls.
        dismiss()
        try {
            if (canCall()) context.startActivity(contactCallIntent(number))
            else {
                open = true
                message("Phone permission is required to call a ride contact.")
            }
        } catch (_: ActivityNotFoundException) {
            open = true
            message("No calling app is available on this device.")
        } catch (_: SecurityException) {
            open = true
            message("This call could not be placed. Check Phone permission in app settings.")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionPending = false
        val number = pendingCallNumber
        if (granted) {
            pendingCallNumber = null
            permissionNeedsSettings = false
            if (number != null) call(number)
        } else if (open && number != null) {
            permissionNeedsSettings = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE)
            permissionHelp = true
        }
    }
    fun requestCallPermission() {
        permissionHelp = false
        permissionPending = true
        permission.launch(Manifest.permission.CALL_PHONE)
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        settingsOpen = false
        // Returning from Settings never replays an earlier call request.
        pendingCallNumber = null
        if (canCall()) {
            permissionNeedsSettings = false
            message("Phone permission enabled. Tap a contact to call.")
        }
    }
    LaunchedEffect(store) {
        try { contacts = withContext(Dispatchers.IO) { store.load() } }
        catch (_: Exception) { message("Saved contacts could not be loaded. Please add them again.") }
        finally {
            busy = false
            contactsLoaded.complete(Unit)
        }
    }
    fun save(updated: List<RideContact>) {
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(updated) }
                contacts = updated
            } catch (_: Exception) { message("Could not save contacts. Please try again.") }
            finally { busy = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        when {
            result.resultCode != Activity.RESULT_OK -> pickerOpen = false
            uri == null -> {
                pickerOpen = false
                message("No contact was selected. Please try again.")
            }
            else -> {
                busy = true
                scope.launch {
                    // A restored picker result can arrive before saved contacts finish loading.
                    contactsLoaded.await()
                    busy = true
                    try {
                        val contact = withContext(Dispatchers.IO) { store.readSelection(uri) }
                        if (contacts.any { PhoneNumberUtils.compare(it.number, contact.number) }) {
                            message("This phone number is already in your ride contacts.")
                        } else if (contacts.size < 4) {
                            val updated = contacts + contact
                            withContext(Dispatchers.IO) { store.save(updated) }
                            contacts = updated
                        }
                    } catch (_: Exception) { message("Could not read or save this contact. Please select it again.") }
                    finally {
                        busy = false
                        pickerOpen = false
                    }
                }
            }
        }
    }
    IconButton(onClick = { open = true }, modifier = modifier.size(72.dp)) {
        Icon(painterResource(R.drawable.ic_contacts), "Contacts", tint = Color.White,
            modifier = Modifier.size(30.dp))
    }
    if (open) ContactsOverlay(contacts, busy, onDismiss = ::dismiss, onAdd = {
        if (!busy && !pickerOpen) {
            try {
                pickerOpen = true
                picker.launch(Intent(Intent.ACTION_PICK).apply { type = Phone.CONTENT_TYPE })
            } catch (_: ActivityNotFoundException) {
                pickerOpen = false
                message("No contact picker is available on this device.")
            } catch (_: SecurityException) {
                pickerOpen = false
                message("Contact access is unavailable on this device.")
            }
        }
    }, onCall = { contact ->
        if (open && !busy && !pickerOpen && !permissionPending && !permissionHelp && !settingsOpen) {
            pendingCallNumber = contact.number
            when {
                canCall() -> call(contact.number)
                activity == null -> {
                    pendingCallNumber = null
                    message("Phone permission could not be requested. Please reopen RideWave.")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.CALL_PHONE
                ) -> {
                    permissionNeedsSettings = false
                    permissionHelp = true
                }
                permissionNeedsSettings -> permissionHelp = true
                else -> requestCallPermission()
            }
        }
    }, onRemove = { contact -> save(contacts - contact) },
        timerPaused = busy || pickerOpen || permissionPending || permissionHelp || settingsOpen)

    if (open && permissionHelp) AlertDialog(
        onDismissRequest = {
            permissionHelp = false
            pendingCallNumber = null
        },
        title = { Text("Allow calls") },
        text = {
            Text(if (permissionNeedsSettings)
                "To call from a contact bubble, enable Phone permission in RideWave's app settings. Then return and tap the contact again."
            else "Allow Phone permission to call your selected contact directly from their bubble.")
        },
        confirmButton = {
            TextButton(onClick = {
                if (permissionNeedsSettings) {
                    permissionHelp = false
                    pendingCallNumber = null
                    settingsOpen = true
                    try {
                        settings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)))
                    } catch (_: ActivityNotFoundException) {
                        settingsOpen = false
                        message("App settings are unavailable on this device.")
                    } catch (_: SecurityException) {
                        settingsOpen = false
                        message("App settings could not be opened.")
                    }
                } else requestCallPermission()
            }) { Text(if (permissionNeedsSettings) "Open settings" else "Allow calls") }
        },
        dismissButton = {
            TextButton(onClick = {
                permissionHelp = false
                pendingCallNumber = null
            }) { Text("Not now") }
        }
    )
}

internal fun contactCallIntent(number: String) =
    Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

