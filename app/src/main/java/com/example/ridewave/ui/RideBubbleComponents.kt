package com.example.ridewave.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.painter.Painter

@Composable
internal fun RideBubbleButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(72.dp)) {
        Icon(painter, contentDescription, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
internal fun RideBubbleSurface(
    diameter: Dp,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    borderAlpha: Float = 0.32f,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Black,
        border = BorderStroke(1.dp, Color.White.copy(alpha = borderAlpha)),
        modifier = modifier.size(diameter).semantics {
            this.contentDescription = contentDescription
        }
    ) {
        Box(Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Column(content = content)
        }
    }
}
