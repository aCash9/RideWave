package com.example.ridewave.ui

import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ridewave.R
import com.example.ridewave.notifications.DeviceNotificationService
import kotlinx.coroutines.delay

@Composable
fun NotificationButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var open by remember { mutableStateOf(false) }
    var access by remember { mutableStateOf(false) }
    val snapshot by DeviceNotificationService.snapshot.collectAsState()
    DisposableEffect(lifecycle, context) {
        fun checkAccess() {
            access = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            if (access && !DeviceNotificationService.snapshot.value.connected) {
                NotificationListenerService.requestRebind(ComponentName(context, DeviceNotificationService::class.java))
            }
        }
        checkAccess()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkAccess()
            if (event == Lifecycle.Event.ON_STOP) open = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Box(modifier) {
        IconButton(onClick = { open = !open }, modifier = Modifier.size(if (portrait) 80.dp else 72.dp)) {
            Icon(painterResource(R.drawable.ic_notifications), "Notifications", tint = Color.White,
                modifier = Modifier.size(if (portrait) 38.dp else 30.dp))
        }
        if (open) {
            NotificationPopup(access, snapshot.connected, snapshot.items, onDismiss = { open = false }) {
                open = false
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, "Notification access settings are unavailable on this device.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
internal fun NotificationPopup(
    access: Boolean,
    connected: Boolean,
    notifications: List<com.example.ridewave.notifications.DeviceNotification>,
    onDismiss: () -> Unit,
    onEnableAccess: () -> Unit
) {
    var interaction by remember { mutableLongStateOf(0L) }
    var touching by remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismiss)
    val timeout = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        5_000L, containsIcons = true, containsText = true, containsControls = true
    ) ?: 5_000L
    LaunchedEffect(interaction, touching, timeout) {
        if (!touching) {
            delay(timeout)
            dismiss()
        }
    }
    val configuration = LocalConfiguration.current
    val popupModifier = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Modifier
            .width((configuration.screenWidthDp * 0.5f).dp)
            .fillMaxHeight()
            .padding(12.dp)
    } else {
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .height((configuration.screenHeightDp * 0.5f).dp)
    }
    Popup(alignment = Alignment.TopEnd, onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)) {
        Surface(color = Color(0xFF202020), contentColor = Color.White,
            shape = MaterialTheme.shapes.large, shadowElevation = 8.dp,
            modifier = popupModifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            interaction++
                            touching = event.changes.any { it.pressed }
                        }
                    }
                }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        Text("Notifications", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Close", color = Color.White) }
                }
                HorizontalDivider(color = Color.DarkGray)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when {
                        !access -> item {
                            Text("Allow notification access to show your device notifications here.")
                            TextButton(onClick = onEnableAccess) { Text("Enable notification access", color = Color.White) }
                        }
                        !connected -> item {
                            Text("Waiting for notification access. If this continues, turn RideWave access off and on in Settings.")
                            TextButton(onClick = onEnableAccess) { Text("Open settings", color = Color.White) }
                        }
                        notifications.isEmpty() -> item { Text("No active notifications") }
                        else -> items(notifications, key = { it.key }) { notification ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val icon = notification.icon
                                    if (icon != null) {
                                        Image(icon.asImageBitmap(), contentDescription = notification.app,
                                            modifier = Modifier.size(36.dp))
                                    } else {
                                        Icon(painterResource(R.drawable.ic_notifications),
                                            contentDescription = notification.app, tint = Color.White,
                                            modifier = Modifier.size(36.dp))
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (notification.title.isNotBlank()) Text(notification.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(notification.text.ifBlank { "No preview available" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = if (notification.title.isBlank()) 4 else 3,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
