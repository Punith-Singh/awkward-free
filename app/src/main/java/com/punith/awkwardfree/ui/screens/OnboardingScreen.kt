package com.punith.awkwardfree.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.ui.theme.*

private data class IntroPage(val eyebrow: String, val title: String, val description: String)

@Composable internal fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        IntroPage("SHARED EXPENSES", "Money between friends.", "Picked up lunch for a friend? Keep a tab on who owes what."),
        IntroPage("VOICE ENTRY", "Just say it, then save it.", "Log a tab by voice or type it in. Review the details and choose a due date before saving."),
        IntroPage("FRIENDLY REMINDERS", "Reminders that sound like you.", "Edit a friendly reminder and send it yourself on WhatsApp. Mark the tab settled when they pay you back.")
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    fun goTo(index: Int) {
        scope.launch { pager.animateScrollToPage(index, animationSpec = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    }
    BackHandler(page > 0) { goTo(page - 1) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Background).safeDrawingPadding()) {
        val screenHeight = maxHeight
        val contentWidth = maxWidth.coerceAtMost(544.dp)
        val sceneHeight = ((contentWidth - 44.dp) * .66f).coerceAtMost(screenHeight * .36f)
        Column(
            Modifier.align(Alignment.TopCenter).width(contentWidth).fillMaxHeight()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Awkward-Free", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onFinish) { Text("Skip", color = Muted) }
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
              Box(Modifier.fillMaxWidth()) {
                FriendsScene(
                    position = { pager.currentPage + pager.currentPageOffsetFraction },
                    settledPage = pager.settledPage,
                    modifier = Modifier.fillMaxWidth().height(sceneHeight).clip(RoundedCornerShape(22.dp))
                )
                // The transparent upper part of each page lets a swipe start on the illustration.
                HorizontalPager(state = pager, verticalAlignment = Alignment.Top) { index ->
                    val current = pages[index]
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(sceneHeight + 32.dp))
                        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp).graphicsLayer {
                            val distance = kotlin.math.abs(pager.currentPage + pager.currentPageOffsetFraction - index)
                            alpha = 1f - .55f * distance.coerceIn(0f, 1f)
                        }) {
                            Text(current.eyebrow, color = Violet, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.35.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(current.title, color = Ink, fontSize = 36.sp, lineHeight = 40.sp,
                                letterSpacing = (-1.2).sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(12.dp))
                            Text(current.description, color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
                        }
                    }
                }
              }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    pages.indices.forEach { index ->
                        val width by animateDpAsState(if (index == page) 24.dp else 7.dp, label = "page indicator")
                        Box(Modifier.width(width).height(6.dp).background(
                            if (index == page) Violet else Violet.copy(alpha = .2f), CircleShape))
                    }
                }
                Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    if (page > 0) TextButton(onClick = { goTo(page - 1) }) { Text("Back", color = Muted) }
                }
            }
            Button(onClick = { if (page == pages.lastIndex) onFinish() else goTo(page + 1) },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                Text(if (page == pages.lastIndex) "Get started" else "Continue", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
