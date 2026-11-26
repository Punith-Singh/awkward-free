package com.punith.awkwardfree.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.model.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.util.*
import java.time.LocalDate

@Composable internal fun Heading(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(eyebrow.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Violet, letterSpacing = .6.sp); Text(title, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) }
}
internal fun Modifier.softCard(shape: Shape = RoundedCornerShape(20.dp), elevation: Dp = 4.dp): Modifier =
    shadow(elevation, shape, ambientColor = Violet.copy(alpha = .10f), spotColor = Violet.copy(alpha = .12f))
        .background(Color.White, shape)
        .border(1.dp, CardBorder, shape)

@Composable internal fun StatusChip(text: String, background: Color, foreground: Color) {
    Row(
        Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).background(foreground, CircleShape))
        Text(text, color = foreground, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable internal fun BackHeader(label: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = back, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Go back", tint = Ink, modifier = Modifier.size(22.dp)) }
        Text(label, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}
@Composable internal fun Pill(text: String, bg: Color, fg: Color, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.background(bg, RoundedCornerShape(50)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 7.dp)
    Text(text, modifier, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
}
@Composable internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp)
            .shadow(6.dp, shape, ambientColor = Violet.copy(alpha = .20f), spotColor = Violet.copy(alpha = .25f))
            .background(Brush.horizontalGradient(listOf(Violet, Color(0xFF8566FF))), shape),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
        shape = shape
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

private val AvatarTints = listOf(
    Color(0xFFFFE5D9) to Color(0xFFAA4F31),
    Color(0xFFE4DFFF) to Color(0xFF5B45C5),
    Color(0xFFDDF6EA) to Color(0xFF116A47),
    Color(0xFFFFF0D2) to Color(0xFF865300),
    Color(0xFFDCEBFF) to Color(0xFF245AA8),
    Color(0xFFFCE1F0) to Color(0xFF9B286F)
)

@Composable internal fun Avatar(name: String) {
    val (background, foreground) = AvatarTints[(name.trim().lowercase().hashCode() and Int.MAX_VALUE) % AvatarTints.size]
    Box(Modifier.size(45.dp).background(background, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), color = foreground, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}
@Composable internal fun DueCard(d: Due, onClick: () -> Unit, onNudge: () -> Unit) {
    val today = LocalDate.now()
    val dueDate = runCatching { LocalDate.parse(d.date) }.getOrNull()
    val isOverdue = !d.settled && dueDate?.isBefore(today) == true
    val dueSoon = !d.settled && dueDate != null && !isOverdue && !dueDate.isAfter(today.plusDays(6))
    Column(Modifier.fillMaxWidth().softCard(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(d.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(d.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(d.reason, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(money(if (d.settled) d.amount else d.remaining().toPlainString()), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1)
                Text(if (d.settled) "Settled" else if (d.owedByMe) "You owe" else "Owed to you", color = Muted, fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                when {
                    d.settled -> StatusChip("Settled", SuccessSoft, Success)
                    isOverdue -> StatusChip("Overdue · ${dateLabel(d.date)}", DangerSoft, Danger)
                    dueSoon -> StatusChip("Due soon · ${dateLabel(d.date)}", WarnSoft, Warn)
                    else -> Text("Due ${dateLabel(d.date)}", color = Muted, fontSize = 12.sp)
                }
            }
            if (!d.settled && !d.owedByMe) TextButton(onClick = onNudge) { Text("Remind") }
            else TextButton(onClick = onClick) { Text("Details") }
        }
    }
}
@Composable internal fun EmptyCard(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().softCard(RoundedCornerShape(20.dp), 2.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) } }
@Composable internal fun Input(value: String, change: (String) -> Unit, label: String, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    var focused by remember { mutableStateOf(false) }
    val ring by animateColorAsState(if (focused) Violet else CardBorder, label = "input focus")
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().background(Color.White, shape)
            .border(if (focused) 1.5.dp else 1.dp, ring, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (focused) Violet else Muted)
        BasicTextField(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }, singleLine = true,
            textStyle = TextStyle(fontFamily = Jakarta, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink),
            cursorBrush = SolidColor(Violet),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = Muted, fontSize = 15.sp); inner() } })
    }
}
@Composable internal fun EmptyFallback(home: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { EmptyCard("Tab not found", "It may have been deleted."); Spacer(Modifier.height(16.dp)); PrimaryButton("Back to home", home) } }
