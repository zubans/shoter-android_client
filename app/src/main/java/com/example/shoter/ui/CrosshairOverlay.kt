package com.example.shoter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CrosshairOverlay(
    modifier: Modifier = Modifier,
    isPlayerDetected: Boolean = false
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(80.dp)
        ) {
            val color = if (isPlayerDetected) Color.Red else Color.White
            drawCrosshair(color)
        }
    }
}

private fun DrawScope.drawCrosshair(color: Color) {
    val strokeWidth = 4.dp.toPx()
    val crosshairSize = 40.dp.toPx()
    val center = Offset(size.width / 2, size.height / 2)
    
    // Draw vertical line
    drawLine(
        color = color,
        start = Offset(center.x, center.y - crosshairSize / 2),
        end = Offset(center.x, center.y + crosshairSize / 2),
        strokeWidth = strokeWidth
    )
    
    // Draw horizontal line
    drawLine(
        color = color,
        start = Offset(center.x - crosshairSize / 2, center.y),
        end = Offset(center.x + crosshairSize / 2, center.y),
        strokeWidth = strokeWidth
    )
    
    // Draw center circle
    drawCircle(
        color = color,
        radius = 3.dp.toPx(),
        center = center,
        style = Stroke(width = strokeWidth)
    )
}
