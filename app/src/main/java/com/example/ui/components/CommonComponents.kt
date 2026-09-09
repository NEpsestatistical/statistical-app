package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GainGreen
import com.example.ui.theme.LossRed

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    background(
        brush = Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun PriceChangePill(percentChange: Double?, changeAmount: Double?) {
    val isPositive = (percentChange ?: 0.0) >= 0
    val color = if (isPositive) GainGreen else LossRed
    val sign = if (isPositive) "+" else ""

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$sign${percentChange?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "—"}%",
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun Sparkline(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true
) {
    if (dataPoints.size < 2) {
        Box(modifier = modifier)
        return
    }
    val lineColor = if (isPositive) GainGreen else LossRed

    Canvas(modifier = modifier) {
        val min = dataPoints.minOrNull() ?: 0.0
        val max = dataPoints.maxOrNull() ?: 1.0
        val range = if (max - min == 0.0) 1.0 else max - min
        val width = size.width
        val height = size.height

        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = index.toFloat() / (dataPoints.size - 1) * width
            val y = height - ((value - min) / range).toFloat() * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
