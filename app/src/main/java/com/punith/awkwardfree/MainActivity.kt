package com.punith.awkwardfree

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val Background = Color(0xFFF5F4FB)
private val Ink = Color(0xFF18152E)
private val Muted = Color(0xFF78768D)
private val Violet = Color(0xFF674FE8)
private val Deep = Color(0xFF241D50)
private val Lavender = Color(0xFFE9E4FF)
private val Lime = Color(0xFFC9F27A)
private val Line = Color(0xFFEBEAF2)
private val Peach = Color(0xFFFFE5D9)

private data class Due(
    val id: String = UUID.randomUUID().toString(), val name: String, val amount: String,
    val reason: String, val date: String, val phone: String = "", val settled: Boolean = false,
    val lastNudged: Long = 0L
)
private class Store(activity: ComponentActivity) {
    private val prefs = activity.getSharedPreferences("awkward_free", 0)
    var upi: String
        get() = prefs.getString("upi", "") ?: ""
        set(value) { prefs.edit().putString("upi", value).apply() }
    var displayName: String
        get() = prefs.getString("display_name", "") ?: ""
        set(value) { prefs.edit().putString("display_name", value).apply() }
    fun read(): List<Due> = try {
        val data = JSONArray(prefs.getString("dues", "[]"))
        (0 until data.length()).map { i -> data.getJSONObject(i).let { o ->
            Due(o.getString("id"), o.getString("name"), o.getString("amount"), o.getString("reason"), o.getString("date"), o.optString("phone"), o.optBoolean("settled"), o.optLong("lastNudged"))
        } }
    } catch (_: Exception) { emptyList() }
    fun write(dues: List<Due>) {
        val data = JSONArray()
        dues.forEach { d -> data.put(JSONObject().put("id", d.id).put("name", d.name).put("amount", d.amount).put("reason", d.reason).put("date", d.date).put("phone", d.phone).put("settled", d.settled).put("lastNudged", d.lastNudged)) }
        prefs.edit().putString("dues", data.toString()).apply()
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        window.statusBarColor = android.graphics.Color.rgb(245, 244, 251)
        window.navigationBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Violet, onPrimary = Color.White, surface = Color.White, background = Background, onSurface = Ink)) { AwkwardApp(store) } }
    }
}
private fun money(amount: String): String = "₹" + try { BigDecimal(amount).stripTrailingZeros().toPlainString() } catch (_: Exception) { amount }
private fun dateLabel(date: String): String = try { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) } catch (_: Exception) { date }
private fun nudgeLabel(time: Long): String = if (time == 0L) "Not nudged yet" else "Opened " + java.text.SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(java.util.Date(time))
private fun upiLink(upi: String, due: Due): String = Uri.Builder().scheme("upi").authority("pay").appendQueryParameter("pa", upi).appendQueryParameter("am", due.amount).appendQueryParameter("cu", "INR").appendQueryParameter("tn", due.reason).build().toString()
private fun message(d: Due, upi: String, tone: String): String {
    val intro = when (tone) {
        "Playful" -> "Hey ${d.name}! My UPI tab says it misses ${money(d.amount)} 😄 Mind settling up for ${d.reason} when you get a chance?"
        "Direct" -> "Hi ${d.name}, could you please send me ${money(d.amount)} for ${d.reason}? Thanks!"
        else -> "Hey ${d.name}! Just a gentle reminder about ${money(d.amount)} for ${d.reason}. Whenever you get a chance, could you send it over? Thanks!"
    }
    return if (upi.isBlank()) "$intro\n\nLet me know if you need my payment details." else "$intro\n\nPay via UPI: ${upiLink(upi, d)}\nUPI ID: $upi"
}

@Composable private fun AwkwardApp(store: Store) {
    var dues by remember { mutableStateOf(store.read()) }
    var upi by remember { mutableStateOf(store.upi) }
    var name by remember { mutableStateOf(store.displayName) }
    var screen by remember { mutableStateOf("home") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var returnToNudge by remember { mutableStateOf(false) }
    val selected = dues.firstOrNull { it.id == selectedId }
    fun update(items: List<Due>) { dues = items; store.write(items) }
    fun navigate(target: String) { editing = false; returnToNudge = false; screen = target }
    fun back() { screen = when (screen) { "details" -> if (selected?.settled == true) "settled" else "dues"; "nudge" -> "details"; "add" -> if (editing && selected != null) "details" else "home"; "settings" -> if (returnToNudge && selected != null) "nudge" else "home"; else -> "home" }; editing = false; returnToNudge = false }
    BackHandler(enabled = screen != "home") { back() }
    Scaffold(containerColor = Background, bottomBar = { BottomBar(screen) { navigate(it) } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                "home" -> HomeScreen(dues, name, { navigate("dues") }, { navigate("settled") }, { navigate("add") }, { d -> selectedId = d.id; screen = "details" }, { d -> selectedId = d.id; screen = "nudge" })
                "dues" -> ListScreen("All dues", "Everything that’s still open.", dues.filterNot { it.settled }, false, { back() }, { d -> selectedId = d.id; screen = "details" }, { d -> selectedId = d.id; screen = "nudge" })
                "settled" -> ListScreen("All square.", "Your settled tabs, all in one place.", dues.filter { it.settled }, true, { back() }, { d -> selectedId = d.id; screen = "details" }, {})
                "add" -> AddScreen(if (editing) selected else null, { back() }) { due -> update(if (editing) dues.map { if (it.id == due.id) due else it } else listOf(due) + dues); selectedId = due.id; editing = false; screen = "details" }
                "details" -> if (selected != null) DetailScreen(selected, { back() }, { editing = true; screen = "add" }, { selectedId = selected.id; screen = "nudge" }, { update(dues.map { if (it.id == selected.id) it.copy(settled = !it.settled) else it }) }, { update(dues.filterNot { it.id == selected.id }); selectedId = null; screen = "home" }) else EmptyFallback { screen = "home" }
                "nudge" -> if (selected != null) NudgeScreen(selected, upi, { back() }, { returnToNudge = true; screen = "settings" }) { update(dues.map { if (it.id == selected.id) it.copy(lastNudged = System.currentTimeMillis()) else it }); screen = "details" } else EmptyFallback { screen = "home" }
                "settings" -> SettingsScreen(upi, name, { back() }) { newUpi, newName -> upi = newUpi; name = newName; store.upi = newUpi; store.displayName = newName; screen = if (returnToNudge && selected != null) "nudge" else "home"; returnToNudge = false }
            }
        }
    }
}

@Composable private fun BottomBar(screen: String, onSelect: (String) -> Unit) {
    val active = when (screen) { "details", "nudge" -> "dues"; else -> screen }
    val options = listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("dues", "All dues", Icons.Outlined.ListAlt), Triple("add", "Add a due", Icons.Outlined.AddBox), Triple("settled", "Settled", Icons.Outlined.CheckCircle), Triple("settings", "Settings", Icons.Outlined.AccountCircle))
    Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().height(70.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        options.forEach { (key, label, icon) -> IconButton(onClick = { onSelect(key) }) { Icon(icon, contentDescription = label, tint = if (active == key) Violet else Color(0xFF9893AA), modifier = Modifier.size(25.dp)) } }
    }
}
@Composable private fun Heading(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(eyebrow.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Violet, letterSpacing = .6.sp); Text(title, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) }
}
@Composable private fun BackHeader(label: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = back, modifier = Modifier.background(Color.White, CircleShape).size(36.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Go back", tint = Ink, modifier = Modifier.size(19.dp)) }
        Pill(label.uppercase(), Lavender, Violet)
    }
}
@Composable private fun Pill(text: String, bg: Color, fg: Color, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.background(bg, RoundedCornerShape(50)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 7.dp)
    Text(text, modifier, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
}
@Composable private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = RoundedCornerShape(17.dp)) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}
@Composable private fun Avatar(name: String) {
    Box(Modifier.size(45.dp).background(Peach, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Text(name.trim().take(1).uppercase(), color = Color(0xFFCC6F50), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
}
@Composable private fun DueCard(d: Due, onClick: () -> Unit, onNudge: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(d.name); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(d.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(d.reason, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Column(horizontalAlignment = Alignment.End) { Text(money(d.amount), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text(if (d.settled) "Settled" else "Due ${dateLabel(d.date)}", color = Muted, fontSize = 11.sp) }
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (d.settled) "Marked settled" else nudgeLabel(d.lastNudged), color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f)); Pill(if (d.settled) "View details" else "Nudge ↗", Lavender, Violet, if (d.settled) onClick else onNudge) }
    }
}
@Composable private fun BalanceCard(dues: List<Due>) {
    val open = dues.filterNot { it.settled }; val total = open.fold(BigDecimal.ZERO) { acc, d -> acc + (d.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Pill("YOUR BALANCE", Color(0xFF4D427C), Color(0xFFE4DFFF)); Text("✦", color = Lime, fontSize = 21.sp) }
        Text("You’re owed", color = Color(0xFFC8C2E0), fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(money(total.toPlainString()), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 33.sp); Pill("${open.size} OPEN", Lime, Deep) }
        Text("Good friends. Clear tabs.", color = Color(0xFFD9D3EE), fontSize = 12.sp)
    }
}
@Composable private fun HomeScreen(dues: List<Due>, name: String, all: () -> Unit, settled: () -> Unit, add: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit) {
    val open = dues.filterNot { it.settled }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("GOOD TO SEE YOU", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(if (name.isBlank()) "Hey there ✨" else "Hey, ${name.trim()} ✨", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }; Box(Modifier.size(40.dp).background(Violet, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Text(name.trim().take(1).uppercase().ifEmpty { "✦" }, color = Color.White, fontWeight = FontWeight.Bold) } } }
        item { BalanceCard(dues) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Your tabs", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink); TextButton(onClick = all) { Text("See all", fontWeight = FontWeight.Bold) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("Owed to me  ${open.size}", Ink, Color.White, all); Pill("Settled  ${dues.count { it.settled }}", Color.White, Muted, settled) } }
        if (open.isEmpty()) item { EmptyCard("Nothing to chase ✨", "Add a due and make asking easier.") }
        items(open.take(2), key = { it.id }) { d -> DueCard(d, { details(d) }, { nudge(d) }) }
        item { PrimaryButton("＋   Add a due", add) }
    }
}
@Composable private fun EmptyCard(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) } }
@Composable private fun ListScreen(title: String, subtitle: String, dues: List<Due>, settled: Boolean, back: () -> Unit, details: (Due) -> Unit, nudge: (Due) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { BackHeader(if (settled) "Settled" else "Your tabs", back) }
        item { Heading(if (settled) "GOOD FRIENDS. CLEAR TABS." else "KEEP TRACK", title, subtitle) }
        item { Pill("${dues.size} ${if (settled) "SETTLED" else "OPEN"}", Lavender, Violet) }
        if (dues.isEmpty()) item { EmptyCard(if (settled) "No settled tabs yet" else "No open tabs", if (settled) "Tabs you mark as settled will appear here." else "Add a due to get started.") }
        items(dues, key = { it.id }) { d -> DueCard(d, { details(d) }, { nudge(d) }) }
    }
}
@Composable private fun Input(value: String, change: (String) -> Unit, label: String, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(), label = { Text(label, fontSize = 12.sp) }, placeholder = { Text(placeholder, color = Muted) }, singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, unfocusedBorderColor = Color.Transparent, focusedBorderColor = Violet))
}
@Composable private fun AddScreen(existing: Due?, back: () -> Unit, save: (Due) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }; var amount by remember(existing?.id) { mutableStateOf(existing?.amount ?: "") }; var reason by remember(existing?.id) { mutableStateOf(existing?.reason ?: "") }; var date by remember(existing?.id) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }; var phone by remember(existing?.id) { mutableStateOf(existing?.phone ?: "") }; var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader(if (existing == null) "New tab" else "Edit tab", back)
        Heading("Keep it simple", if (existing == null) "Add a little tab." else "Edit your tab.", "Just the details you need to remember.")
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(22.dp)).padding(18.dp)) { Text("HOW MUCH?", color = Color(0xFFD2C9FF), fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(if (amount.isBlank()) "₹ 0" else money(amount), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold); Text("Every rupee deserves its way home.", color = Color(0xFFDCD5FF), fontSize = 12.sp) }
        Input(amount, { amount = it }, "Amount (₹)", "500", KeyboardType.Decimal)
        Input(name, { name = it }, "Who owes you?", "Ananya")
        Input(reason, { reason = it }, "What was it for?", "Dinner at Toit")
        val parsed = try { LocalDate.parse(date) } catch (_: Exception) { LocalDate.now() }
        OutlinedButton(onClick = { DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, parsed.year, parsed.monthValue - 1, parsed.dayOfMonth).show() }, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White), border = null) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Due  ${dateLabel(date)}", color = Ink); Text("Calendar", color = Violet) } }
        Input(phone, { phone = it }, "WhatsApp number (optional)", "919876543210", KeyboardType.Phone)
        Text("Include the country code, digits only. Or choose a contact in WhatsApp.", color = Muted, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        PrimaryButton(if (existing == null) "Save this tab" else "Save changes", {
            val n = amount.toBigDecimalOrNull()
            error = when { name.isBlank() || reason.isBlank() -> "Add a name and reason."; n == null || n <= BigDecimal.ZERO || n.scale() > 2 || n > BigDecimal("10000000") -> "Enter a valid amount greater than zero with up to 2 decimal places."; phone.isNotBlank() && !phone.matches(Regex("[1-9][0-9]{7,14}")) -> "Enter the phone number with country code, using digits only."; else -> "" }
            if (error.isEmpty()) save((existing ?: Due(name = "", amount = "", reason = "", date = date)).copy(name = name.trim(), amount = n!!.setScale(2).toPlainString(), reason = reason.trim(), date = date, phone = phone))
        })
    }
}
@Composable private fun DetailScreen(d: Due, back: () -> Unit, edit: () -> Unit, nudge: () -> Unit, toggle: () -> Unit, delete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader(if (d.settled) "Settled" else "Open tab", back)
        Heading("The little details", "${d.name}’s tab.", d.reason)
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(24.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Pill(if (d.settled) "SETTLED" else "OPEN TAB", Color(0xFF4D427C), Color(0xFFE4DFFF)); Text("✦", color = Lime, fontSize = 21.sp) }
            Text("AMOUNT OWED", color = Color(0xFFD2C9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(money(d.amount), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            Text("Due ${dateLabel(d.date)}", color = Color(0xFFDCD5FF), fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TAB ACTIVITY", color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).background(Lavender, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Schedule, null, tint = Violet) }; Spacer(Modifier.width(12.dp)); Column { Text(if (d.lastNudged == 0L) "No nudges yet" else nudgeLabel(d.lastNudged), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(if (d.lastNudged == 0L) "A friendly reminder is a good start." else "WhatsApp was opened with your message.", color = Muted, fontSize = 12.sp) } }
        }
        if (!d.settled) PrimaryButton("Send a nudge", nudge)
        OutlinedButton(onClick = toggle, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Ink), border = null) { Text(if (d.settled) "Reopen tab" else "✓   Mark as settled", fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = edit) { Text("Edit details") }; TextButton(onClick = { confirmDelete = true }) { Text("Delete tab", color = Muted) } }
        Text("Once you’ve confirmed the payment, mark this tab as settled.", color = Muted, fontSize = 12.sp)
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete this tab?") }, text = { Text("This removes ${d.name}'s ${money(d.amount)} entry from this device.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; delete() }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}
@Composable private fun NudgeScreen(d: Due, upi: String, back: () -> Unit, settings: () -> Unit, onOpened: () -> Unit) {
    val context = LocalContext.current
    var tone by remember(d.id, upi) { mutableStateOf("Friendly") }; var text by remember(d.id, upi) { mutableStateOf(message(d, upi, "Friendly")) }; var error by remember { mutableStateOf("") }; var confirmTone by remember { mutableStateOf<String?>(null) }
    fun chooseTone(option: String) { tone = option; text = message(d, upi, option) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        BackHeader("Almost there", back)
        Heading("Let’s keep it easy", "A little nudge.", "Choose your vibe and make it yours.")
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(19.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Avatar(d.name); Spacer(Modifier.width(12.dp)); Column { Text("${d.name} owes you", color = Muted, fontSize = 12.sp); Text("${money(d.amount)}  ·  ${d.reason}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp) } }
        Text("PICK A TONE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Friendly", "Playful", "Direct").forEach { option -> Pill(option, if (tone == option) Violet else Color.White, if (tone == option) Color.White else Ink) { if (text != message(d, upi, tone)) confirmTone = option else chooseTone(option) } } }
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MESSAGE PREVIEW  ·  TAP TO EDIT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Line)
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Violet))
            if (upi.isNotBlank()) Column(Modifier.fillMaxWidth().background(Color(0xFFF4F0FF), RoundedCornerShape(14.dp)).padding(12.dp)) { Text("PAYMENT LINK INCLUDED", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(upi, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold) } else TextButton(onClick = settings) { Text("Add UPI ID for a payment link") }
        }
        Text("You’ll review and send it yourself in WhatsApp. Opening WhatsApp records the nudge time; it doesn't confirm delivery.", color = Muted, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        PrimaryButton("Continue to WhatsApp", {
            if (text.isBlank()) error = "Write a message first." else try {
                val url = "https://wa.me/${d.phone}?text=${Uri.encode(text)}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                onOpened()
            } catch (_: Exception) { error = "Couldn't open WhatsApp. Check that WhatsApp or a browser is installed." }
        })
    }
    confirmTone?.let { next -> AlertDialog(onDismissRequest = { confirmTone = null }, title = { Text("Change the tone?") }, text = { Text("This replaces the edits you made to the message.") }, confirmButton = { TextButton(onClick = { chooseTone(next); confirmTone = null }) { Text("Change tone") } }, dismissButton = { TextButton(onClick = { confirmTone = null }) { Text("Keep message") } }) }
}
@Composable private fun SettingsScreen(currentUpi: String, currentName: String, back: () -> Unit, save: (String, String) -> Unit) {
    var upi by remember { mutableStateOf(currentUpi) }; var name by remember { mutableStateOf(currentName) }; var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader("Your settings", back)
        Heading("Make it yours", "Payment settings.", "Make it easy for friends to pay.")
        Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("YOUR UPI DETAILS", color = Color(0xFFD2C9FF), fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Where should they pay you?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text("Your UPI ID is included in your payment link and message.", color = Color(0xFFDCD5FF), fontSize = 13.sp) }
        Input(name, { name = it }, "Your name (optional)", "Punith")
        Input(upi, { upi = it }, "Your UPI ID", "yourname@upi")
        Text("Double-check your UPI ID before sharing. Your data stays on this device.", color = Muted, fontSize = 12.sp)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Save settings", { val value = upi.trim(); if (value.isNotEmpty() && !value.matches(Regex("[A-Za-z0-9._-]+@[A-Za-z0-9._-]+"))) error = "Enter a valid UPI ID, like name@bank." else save(value, name.trim()) })
        EmptyCard("Your data, your control", "Dues are stored only on this device. Confirm payments yourself before marking them settled.")
    }
}
@Composable private fun EmptyFallback(home: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { EmptyCard("Tab not found", "It may have been deleted."); Spacer(Modifier.height(16.dp)); PrimaryButton("Back to home", home) } }
