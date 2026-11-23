package com.punith.awkwardfree.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.punith.awkwardfree.ui.theme.Ink
import com.punith.awkwardfree.ui.theme.Lime
import com.punith.awkwardfree.ui.theme.Muted
import com.punith.awkwardfree.ui.theme.Violet

/** A presentation-only celebration; the settlement is saved before this dialog opens. */
@Composable
internal fun SettlementCelebration(name: String, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(1900, easing = FastOutSlowInEasing))
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, tonalElevation = 8.dp) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Canvas(
                    Modifier.fillMaxWidth().height(210.dp)
                        .background(Color(0xFFF4F1FF), RoundedCornerShape(22.dp))
                        .semantics { contentDescription = "Coins and a receipt move into a wallet" }
                ) {
                    val w = size.width
                    val h = size.height
                    val destination = Offset(w * .5f, h * .67f)
                    val t = progress.value.coerceIn(0f, 1f)
                    val points = listOf(Offset(.18f, .27f), Offset(.79f, .18f), Offset(.78f, .43f), Offset(.30f, .12f))
                    points.forEachIndexed { index, point ->
                        val phase = ((t - index * .06f) / .76f).coerceIn(0f, 1f)
                        val x = w * point.x + (destination.x - w * point.x) * phase
                        val y = h * point.y + (destination.y - h * point.y) * phase
                        val alpha = (1f - (phase - .72f).coerceAtLeast(0f) / .28f).coerceIn(0f, 1f)
                        val radius = w * (.048f - .016f * phase)
                        val color = if (index % 2 == 0) Color(0xFFF2B951) else Color(0xFFC3E994)
                        drawCircle(color.copy(alpha = alpha), radius, Offset(x, y))
                        drawCircle(Color.White.copy(alpha = alpha * .65f), radius * .68f, Offset(x, y), style = Stroke(width = 1.5.dp.toPx()))
                    }
                    val paperPhase = ((t - .1f) / .7f).coerceIn(0f, 1f)
                    val px = w * .49f + (destination.x - w * .49f) * paperPhase
                    val py = h * .18f + (destination.y - h * .18f) * paperPhase
                    val paperAlpha = (1f - (paperPhase - .70f).coerceAtLeast(0f) / .30f).coerceIn(0f, 1f)
                    val pw = w * .105f
                    val ph = h * .17f
                    drawRoundRect(Color.White.copy(alpha = paperAlpha), Offset(px - pw / 2, py - ph / 2), Size(pw, ph), CornerRadius(5.dp.toPx()))
                    repeat(3) { row ->
                        val yy = py - ph * .22f + row * ph * .22f
                        drawLine(Violet.copy(alpha = paperAlpha * .55f), Offset(px - pw * .27f, yy), Offset(px + pw * .27f, yy), 1.5.dp.toPx(), StrokeCap.Round)
                    }
                    // The wallet remains visible after the floating pieces arrive.
                    val left = w * .33f
                    val top = h * .62f
                    val walletSize = Size(w * .34f, h * .20f)
                    drawRoundRect(Color(0xFF352B74), Offset(left, top), walletSize, CornerRadius(14.dp.toPx()))
                    drawRoundRect(Violet, Offset(left, top + h * .035f), walletSize.copy(height = h * .17f), CornerRadius(12.dp.toPx()))
                    drawRoundRect(Color(0xFF5140B5), Offset(w * .53f, top + h * .075f), Size(w * .14f, h * .065f), CornerRadius(6.dp.toPx()))
                    drawCircle(Lime, 3.dp.toPx(), Offset(w * .59f, top + h * .107f))
                }
                Text("All settled!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
                Text("Your tab with $name is settled. Nice and easy.", color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                PrimaryButton("Done", onDismiss)
            }
        }
    }
}
