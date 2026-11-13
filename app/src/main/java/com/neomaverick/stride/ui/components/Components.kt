package com.neomaverick.stride.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neomaverick.stride.ui.theme.StrideColors

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.padding(18.dp)) { content() }
    }

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelMedium, color = StrideColors.Muted)

/** Animated ring with a gradient sweep. [progress] 0..1. */
@Composable
fun ProgressRing(progress: Float, size: Dp, stroke: Dp, modifier: Modifier = Modifier, center: @Composable () -> Unit = {}) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 120f), label = "ring")
    Box(modifier.size(size).semantics { contentDescription = "${(progress * 100).toInt()} percent" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(Color(0x22FFFFFF), -90f, 360f, false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(StrideColors.Teal, StrideColors.Lime, StrideColors.Teal)),
                -90f, 360f * p, false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        center()
    }
}

/** Simple vertical bars, values 0..1, with labels underneath. */
@Composable
fun BarChart(values: List<Float>, labels: List<String>, highlight: Int = values.lastIndex, modifier: Modifier = Modifier) {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size
            val gap = size.width / n * 0.35f
            val w = size.width / n - gap
            values.forEachIndexed { i, v ->
                val h = (size.height * v.coerceIn(0.02f, 1f))
                val x = i * (w + gap) + gap / 2
                drawRoundRect(Color(0x18FFFFFF), Offset(x, 0f), Size(w, size.height), CornerRadius(w / 3))
                drawRoundRect(
                    if (i == highlight) Brush.verticalGradient(listOf(StrideColors.Lime, StrideColors.Teal)) else Brush.verticalGradient(listOf(Color(0xFF5A6B91), Color(0xFF3A4766))),
                    Offset(x, size.height - h), Size(w, h), CornerRadius(w / 3))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            labels.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = StrideColors.Muted) }
        }
    }
}

/** Smooth line with a soft fill, for step counts. */
@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = StrideColors.Teal) {
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(1f)
        val step = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * step, size.height - size.height * (v / max) * 0.9f) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val a = pts[i - 1]; val b = pts[i]; val mx = (a.x + b.x) / 2
                cubicTo(mx, a.y, mx, b.y, b.x, b.y)
            }
        }
        val fill = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(StrideColors.Lime, 5.dp.toPx(), pts.last())
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) =
    Text(text, modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
