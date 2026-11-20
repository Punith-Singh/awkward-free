package com.punith.awkwardfree.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*

@Composable internal fun Heading(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(eyebrow.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Violet, letterSpacing = .6.sp); Text(title, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) }
}
@Composable internal fun BackHeader(label: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = back, modifier = Modifier.background(Color.White, CircleShape).size(36.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Go back", tint = Ink, modifier = Modifier.size(19.dp)) }
        Pill(label.uppercase(), Lavender, Violet)
    }
}
@Composable internal fun Pill(text: String, bg: Color, fg: Color, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.background(bg, RoundedCornerShape(50)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 7.dp)
    Text(text, modifier, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
}
@Composable internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = RoundedCornerShape(17.dp)) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}
@Composable internal fun Avatar(name: String) {
    Box(Modifier.size(45.dp).background(Peach, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Text(name.trim().take(1).uppercase(), color = Color(0xFFCC6F50), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
}
@Composable internal fun DueCard(d: Due, onClick: () -> Unit, onNudge: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(d.name); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(d.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(d.reason, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Column(horizontalAlignment = Alignment.End) { Text(money(d.remaining().toPlainString()), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text(if (d.settled) "Settled" else "Due ${dateLabel(d.date)}", color = Muted, fontSize = 11.sp) }
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (d.settled) "Marked settled" else nudgeLabel(d.lastNudged), color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f)); Pill(if (d.settled) "View details" else "Nudge ↗", Lavender, Violet, if (d.settled) onClick else onNudge) }
    }
}
@Composable internal fun EmptyCard(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) } }
@Composable internal fun Input(value: String, change: (String) -> Unit, label: String, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
        BasicTextField(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(), singleLine = true,
            textStyle = TextStyle(fontFamily = Jakarta, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = Muted, fontSize = 15.sp); inner() } })
    }
}
@Composable internal fun EmptyFallback(home: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { EmptyCard("Tab not found", "It may have been deleted."); Spacer(Modifier.height(16.dp)); PrimaryButton("Back to home", home) } }
