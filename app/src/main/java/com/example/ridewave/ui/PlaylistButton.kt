package com.example.ridewave.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.example.ridewave.spotify.SpotifyWebApi
import kotlinx.coroutines.delay

@Composable
fun PlaylistButton(
    playlists: List<SpotifyWebApi.Playlist>, loading: Boolean, authorized: Boolean,
    selectedIds: Set<String>, onAuthorize: () -> Unit, onLoad: () -> Unit,
    onSave: (Set<String>) -> Unit, onSelect: (SpotifyWebApi.Playlist) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    setupComplete: Boolean = selectedIds.isNotEmpty()
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var interaction by remember { mutableLongStateOf(0L) }
    var touching by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
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
    LaunchedEffect(open, interaction, touching, loading, resumed, timeout) {
        if (open && !touching && !loading && resumed) {
            delay(timeout)
            open = false
        }
    }
    fun interact(action: () -> Unit) {
        interaction++
        action()
    }
    val selected = playlists.filter { it.id in selectedIds }.take(4)
    RideBubbleButton(painterResource(R.drawable.ic_playlist), "Playlists", {
        draft = ArrayList(selectedIds)
        editing = !setupComplete || selectedIds.size > 4
        open = true
        if (authorized) onLoad()
    }, modifier)
    if (open) Dialog(
        onDismissRequest = { open = false },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let {
                it.setDimAmount(0f)
                WindowCompat.getInsetsController(it, view).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            onDispose { }
        }
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    interaction++
                    touching = event.changes.any { it.pressed }
                }
            }
        }) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("PLAYLISTS", color = Color.Gray, modifier = Modifier.weight(1f))
                    if (authorized && !editing) TextButton(onClick = {
                        interact {
                            draft = ArrayList(selectedIds)
                            editing = true
                            onLoad()
                        }
                    }) { Text("Edit") }
                    TextButton(onClick = { open = false }) { Text("Close") }
                }
                error?.let {
                    Text(it, color = Color.White, textAlign = TextAlign.Center)
                    TextButton(onClick = { interact(onLoad) }, enabled = !loading) { Text("Retry") }
                }
                when {
                    loading && (editing || !authorized) -> CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(48.dp))
                    !authorized -> {
                        Text("Connect Spotify to choose up to four playlists.", color = Color.White,
                            modifier = Modifier.padding(24.dp))
                        Button(onClick = { interact(onAuthorize) }) { Text("Connect Web API") }
                    }
                    editing -> {
                        val availableIds = playlists.map { it.id }.toSet()
                        val chosen = draft.filter { it in availableIds }.toSet()
                        Text("Choose up to 4 playlists (${chosen.size}/4)", color = Color.White)
                        if (chosen.size > 4) Text("Remove a playlist to save your four favorites.", color = Color.White)
                        LazyColumn(
                            Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                val checked = playlist.id in chosen
                                OutlinedButton(
                                    onClick = {
                                        interact {
                                            draft = ArrayList(if (checked) chosen - playlist.id else chosen + playlist.id)
                                        }
                                    },
                                    enabled = checked || chosen.size < 4,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                        .semantics { this.selected = checked },
                                    border = BorderStroke(
                                        if (checked) 2.dp else 1.dp,
                                        Color.White.copy(alpha = if (checked) 0.9f else 0.3f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (checked) Color(0xFF303030) else Color.Transparent,
                                        contentColor = Color.White,
                                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(playlist.name, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Button(onClick = { interact { onSave(chosen); editing = false } },
                            enabled = chosen.size <= 4 && error == null) { Text("Done") }
                    }
                    selected.isEmpty() -> Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists selected. Tap Edit to add up to four.",
                            color = Color.White, textAlign = TextAlign.Center)
                    }
                    else -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val bubbles = selected
                        val columns = if (maxWidth > maxHeight) bubbles.size else minOf(2, bubbles.size)
                        val diameter = minOf(180.dp, (maxWidth - 16.dp * (columns - 1)) / columns)
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            bubbles.chunked(columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)) {
                                    row.forEach { playlist ->
                                        PlaylistBubble(playlist, diameter) { interact { onSelect(it) } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
