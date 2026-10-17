package com.example.ridewave.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ridewave.R
import com.example.ridewave.contacts.RideContact
import com.example.ridewave.ui.theme.RideWaveTheme
import kotlinx.coroutines.delay

private val ContactWhite = Color.White
private val ContactSecondary = Color(0xFF999999)

@Composable
internal fun ContactsOverlay(
    contacts: List<RideContact>, busy: Boolean,
    onDismiss: () -> Unit, onAdd: () -> Unit,
    onCall: (RideContact) -> Unit, onRemove: (RideContact) -> Unit,
    timerPaused: Boolean = false
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    var interaction by remember { mutableLongStateOf(0L) }
    var touching by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val dismiss by rememberUpdatedState(onDismiss)
    val opacity = remember { Animatable(0f) }
    val timeout = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        5_000L, containsIcons = true, containsText = true, containsControls = true
    ) ?: 5_000L
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed) touching = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(exiting) {
        opacity.animateTo(if (exiting) 0f else 1f, tween(if (exiting) 120 else 160))
        if (exiting) dismiss()
    }
    LaunchedEffect(interaction, touching, busy, timerPaused, resumed, timeout, exiting) {
        if (!touching && !busy && !timerPaused && resumed && !exiting) {
            delay(timeout)
            exiting = true
        }
    }
    fun interact(action: () -> Unit) {
        if (!exiting) { interaction++; action() }
    }
    Dialog(onDismissRequest = { exiting = true },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // Keep the same immersive presentation as the ride screen in this separate window.
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let {
                it.setDimAmount(0f)
                WindowCompat.getInsetsController(it, view).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            onDispose { }
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity.value }
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        interaction++
                        touching = event.changes.any { it.pressed }
                    }
                }
            }) {
            // A separate scrim owns outside taps, so contacts never pass input to playback.
            Box(Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClickLabel = "Close contacts", onClick = { exiting = true }))
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("RIDE CONTACTS", color = ContactSecondary, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    if (contacts.isNotEmpty()) TextButton(
                        onClick = { interact { editing = !editing } }, enabled = !busy && !exiting,
                        modifier = Modifier.heightIn(min = 56.dp)) {
                        Text(if (editing) "Done" else "Edit", color = ContactWhite)
                    }
                    IconButton(onClick = { exiting = true }, modifier = Modifier.size(56.dp)) {
                        Icon(painterResource(R.drawable.ic_contact_close), "Close contacts",
                            tint = ContactWhite, modifier = Modifier.size(28.dp))
                    }
                }
                ContactBubbleLayout(contacts, editing, busy || exiting, Modifier.weight(1f).fillMaxWidth(),
                    onAdd = { interact(onAdd) },
                    onContact = { contact -> interact { if (editing) onRemove(contact) else onCall(contact) } })
                Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(color = ContactWhite,
                        strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactBubbleLayout(
    contacts: List<RideContact>, editing: Boolean, disabled: Boolean, modifier: Modifier,
    onAdd: () -> Unit, onContact: (RideContact) -> Unit
) {
    val entries: List<RideContact?> = contacts.take(4) + if (contacts.size < 4) listOf(null) else emptyList()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val wide = maxWidth > maxHeight
        val columns = if (wide) entries.size else minOf(entries.size, 2)
        val rows = (entries.size + columns - 1) / columns
        val gap = if (wide) 20.dp else 24.dp
        val diameter = minOf(
            (maxWidth - gap * (columns - 1)) / columns,
            (maxHeight - gap * (rows - 1)) / rows,
            if (entries.size == 1) 248.dp else 208.dp
        ).coerceAtLeast(48.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap)) {
            entries.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEachIndexed { columnIndex, contact ->
                        key(contact?.number ?: "add-contact") {
                            ContactBubble(contact, diameter, rowIndex * columns + columnIndex, editing, disabled) {
                                if (contact == null) onAdd() else onContact(contact)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactBubble(
    contact: RideContact?, diameter: Dp, index: Int, editing: Boolean, disabled: Boolean, onClick: () -> Unit
) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 35L)
        entrance.animateTo(1f, tween(120))
    }
    val photo = remember(contact?.photo) {
        try {
            contact?.photo?.let { Base64.decode(it, Base64.NO_WRAP) }?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
    RideBubbleSurface(
        diameter = diameter,
        contentDescription = when {
            contact == null -> "Add contact"
            editing -> "Remove ${contact.name}"
            else -> "Call ${contact.name}, ${contact.number}"
        },
        onClick = onClick,
        enabled = !disabled,
        borderAlpha = if (contact == null) 0.5f else 0.32f,
        contentPadding = if (photo != null) 0.dp else 16.dp,
        modifier = Modifier.graphicsLayer { alpha = entrance.value.coerceIn(0f, 1f) }
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (contact == null) {
                        Icon(painterResource(R.drawable.ic_contact_add), null, tint = ContactWhite,
                            modifier = Modifier.size((diameter * 0.25f).coerceIn(28.dp, 56.dp)))
                        Text("Add contact", color = ContactSecondary, fontSize = 14.sp,
                            textAlign = TextAlign.Center, maxLines = 2)
                    } else {
                        Text(contact.name, color = ContactWhite, textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold, fontSize = if (diameter >= 160.dp) 24.sp else 20.sp,
                            lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Icon(painterResource(if (editing) R.drawable.ic_contact_remove else R.drawable.ic_contact_call),
                            null, tint = if (editing) Color.White else ContactSecondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Preview(name = "Contacts portrait", widthDp = 393, heightDp = 852)
@Preview(name = "Contacts landscape", widthDp = 852, heightDp = 393)
@Composable
private fun ContactsPreview() {
    RideWaveTheme {
        ContactsOverlay(listOf(RideContact("Alex", "1"), RideContact("Jordan", "2"), RideContact("Sam", "3")),
            false, {}, {}, {}, {}, timerPaused = true)
    }
}
