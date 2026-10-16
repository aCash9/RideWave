package com.example.ridewave.notifications

import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceNotification(val key: String, val app: String, val title: String, val text: String, val icon: Bitmap? = null)

data class NotificationSnapshot(
    val connected: Boolean = false,
    val items: List<DeviceNotification> = emptyList()
)

class DeviceNotificationService : NotificationListenerService() {
    companion object {
        private val mutableSnapshot = MutableStateFlow(NotificationSnapshot())
        val snapshot = mutableSnapshot.asStateFlow()
    }

    override fun onListenerConnected() = refresh()

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onListenerDisconnected() {
        mutableSnapshot.value = NotificationSnapshot()
    }

    override fun onDestroy() {
        mutableSnapshot.value = NotificationSnapshot()
        super.onDestroy()
    }

    private fun refresh() {
        try {
            val icons = mutableMapOf<String, Bitmap?>()
            val items = activeNotifications.orEmpty().sortedByDescending { it.postTime }.map { sbn ->
                val extras = sbn.notification.extras
                val app = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(sbn.packageName, 0)
                    ).toString()
                } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                    sbn.packageName
                }
                val icon = icons.getOrPut(sbn.packageName) {
                    val drawable = try {
                        packageManager.getApplicationIcon(sbn.packageName)
                    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                        sbn.notification.smallIcon?.loadDrawable(this)
                    }
                    drawable?.toBitmap(96, 96)
                }
                DeviceNotification(
                    sbn.key, app,
                    extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                        ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                        ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n"))
                        ?.toString().orEmpty(),
                    icon
                )
            }
            mutableSnapshot.value = NotificationSnapshot(true, items)
        } catch (_: SecurityException) {
            mutableSnapshot.value = NotificationSnapshot()
        }
    }
}
