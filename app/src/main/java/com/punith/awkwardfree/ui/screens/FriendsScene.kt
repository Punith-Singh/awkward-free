package com.punith.awkwardfree.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.punith.awkwardfree.R

/**
 * The illustration is intentionally a single image: its gentle camera movement is
 * animated, while the people in the artwork are not individually rigged.
 */
@Composable
internal fun FriendsScene(position: () -> Float, settledPage: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "Cafe illustration")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Gentle camera movement"
    )

    Image(
        painter = painterResource(R.drawable.onboarding_cafe),
        contentDescription = "Two friends share a coin at a cafe table.",
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer {
            // Keep the crop covered while moving slightly between onboarding pages.
            val page = position().coerceIn(0f, 2f)
            val scale = 1.04f + 0.02f * drift
            scaleX = scale
            scaleY = scale
            translationX = (1f - page) * size.width * 0.012f
            translationY = (drift - 0.5f) * size.height * 0.01f
        }
    )
}
