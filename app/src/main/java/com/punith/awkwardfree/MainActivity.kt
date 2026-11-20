package com.punith.awkwardfree

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import java.util.UUID

private val Ink = Color(0xFF202B25)
private val Green = Color(0xFF235B43)
private val Mint = Color(0xFFD9EEE0)
private val Canvas = Color(0xFFF7F8F4)
private val Muted = Color(0xFF718077)

private data class Due(val id: String = UUID.randomUUID().toString(), val name: String, val amount: String, val reason: String, val date: String, val phone: String = "", val settled: Boolean = false, val lastNudged: Long = 0)
private class Store(activity: ComponentActivity) {
    private val prefs = activity.getSharedPreferences("awkward_free", 0)
    var upi: String
        get() = prefs.getString("upi", "") ?: ""
        set(value) { prefs.edit().putString("upi", value).apply() }
    fun read(): List<Due> = try {
        val array = JSONArray(prefs.getString("dues", "[]")); (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Due(o.getString("id"), o.getString("name"), o.getString("amount"), o.getString("reason"), o.getString("date"), o.optString("phone"), o.optBoolean("settled"), o.optLong("lastNudged"))
        }
    } catch (_: Exception) { emptyList() }
    fun write(dues: List<Due>) {
        val array = JSONArray(); dues.forEach { d -> array.put(JSONObject().put("id", d.id).put("name", d.name).put("amount", d.amount).put("reason", d.reason).put("date", d.date).put("phone", d.phone).put("settled", d.settled).put("lastNudged", d.lastNudged)) }
        prefs.edit().putString("dues", array.toString()).apply()
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val store = Store(this); setContent {
        MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, surface = Color.White, background = Canvas, onSurface = Ink)) { App(store) }
    } }
}
private fun money(amount: String): String = "₹" + try { BigDecimal(amount).stripTrailingZeros().toPlainString() } catch (_: Exception) { amount }
private fun niceDate(date: String): String = try { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy")) } catch (_: Exception) { date }
private fun nudgeDate(time: Long): String = if (time == 0L) "Not nudged yet" else "Last nudged " + java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(time))
private fun upiLink(upi: String, due: Due): String = Uri.Builder().scheme("upi").authority("pay").appendQueryParameter("pa", upi).appendQueryParameter("am", due.amount).appendQueryParameter("cu", "INR").appendQueryParameter("tn", due.reason).build().toString()
private fun message(d: Due, upi: String, tone: String): String {
    val intro = when (tone) {
        "Playful" -> "Hey ${d.name}! My UPI tab says it misses ${money(d.amount)} 😄 Mind settling up for ${d.reason} when you get a chance?"
        "Direct" -> "Hi ${d.name}, could you please send me ${money(d.amount)} for ${d.reason}? Thanks!"
        else -> "Hey ${d.name}! Just a gentle reminder about ${money(d.amount)} for ${d.reason}. Whenever you get a chance, could you send it over? Thanks!"
    }
    return if (upi.isBlank()) "$intro\n\nLet me know if you need my payment details." else "$intro\n\nPay via UPI: ${upiLink(upi, d)}\nUPI ID: $upi"
}
@Composable private fun App(store: Store) {
    var dues by remember { mutableStateOf(store.read()) }
    var upi by remember { mutableStateOf(store.upi) }
    var screen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<Due?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    fun update(new: List<Due>) { dues = new; store.write(new) }
    Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
        when (screen) {
            "add" -> AddScreen(onBack = { screen = "home" }, onSave = { update(listOf(it) + dues); screen = "home" })
            "settings" -> SettingsScreen(upi, { screen = "home" }) { upi = it; store.upi = it; screen = "home" }
            "nudge" -> selected?.let { d -> NudgeScreen(d, upi, { screen = "home" }, { screen = "settings" }) { update(dues.map { if (it.id == d.id) it.copy(lastNudged = System.currentTimeMillis()) else it }); screen = "home" } }
            else -> HomeScreen(dues, tab, { tab = it }, { screen = "add" }, { screen = "settings" }, { selected = it; screen = "nudge" }, { d -> update(dues.map { if (it.id == d.id) it.copy(settled = !it.settled) else it }) }, { d -> update(dues.filterNot { it.id == d.id }) })
        }
    }
}
@Composable private fun HomeScreen(dues: List<Due>, tab: Int, setTab: (Int) -> Unit, add: () -> Unit, settings: () -> Unit, nudge: (Due) -> Unit, toggle: (Due) -> Unit, delete: (Due) -> Unit) {
    var deleting by remember { mutableStateOf<Due?>(null) }
    val open = dues.filterNot { it.settled }; val settled = dues.filter { it.settled }; val visible = if (tab == 0) open else settled
    val total = open.fold(BigDecimal.ZERO) { sum, d -> sum + (d.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    Scaffold(containerColor = Canvas, floatingActionButton = { ExtendedFloatingActionButton(onClick = add, containerColor = Green, contentColor = Color.White, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add a due") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("AWKWARD-FREE", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Spacer(Modifier.height(5.dp)); Text("Hey, let's settle up.", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink) }
                IconButton(onClick = settings) { Icon(Icons.Default.Settings, contentDescription = "Payment settings", tint = Ink) }
            } }
            item { Text("Less chasing. More good vibes.", color = Muted) }
            item { Spacer(Modifier.height(2.dp)); Column(Modifier.fillMaxWidth().background(Green, RoundedCornerShape(24.dp)).padding(24.dp)) {
                Text("TOTAL OWED TO YOU", color = Color(0xFFC5E3D1), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp)); Text(money(total.toPlainString()), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                Spacer(Modifier.height(7.dp)); Text("${open.size} ${if (open.size == 1) "open tab" else "open tabs"} · You've got this", color = Color(0xFFD0E8D8), fontSize = 14.sp)
            } }
            item { Spacer(Modifier.height(7.dp)); Row(Modifier.fillMaxWidth().background(Color(0xFFE9EDE8), RoundedCornerShape(16.dp)).padding(4.dp)) {
                listOf("Owed to me  ${open.size}", "Settled  ${settled.size}").forEachIndexed { i, title -> Box(Modifier.weight(1f).background(if (tab == i) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable { setTab(i) }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) { Text(title, color = if (tab == i) Ink else Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) } }
            } }
            item { Text(if (tab == 0) "YOUR OPEN TABS" else "ALL SETTLED", color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
            if (visible.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(vertical = 35.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (tab == 0) "Nothing to chase ✨" else "All clear so far", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink); Spacer(Modifier.height(8.dp)); Text(if (tab == 0) "Add a due and we'll make asking easier." else "Settled tabs will show up here.", color = Muted) } }
            items(visible, key = { it.id }) { d -> DueCard(d, { nudge(d) }, { toggle(d) }, { deleting = d }) }
        }
    }
    deleting?.let { d -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this tab?") }, text = { Text("This removes ${d.name}'s ${money(d.amount)} entry from this device.") }, confirmButton = { TextButton(onClick = { delete(d); deleting = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
@Composable private fun DueCard(d: Due, nudge: () -> Unit, toggle: () -> Unit, delete: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Mint, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { Text(d.name.take(1).uppercase(), color = Green, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(d.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(d.reason, color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(money(d.amount), fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink)
        }
        Spacer(Modifier.height(14.dp)); Text("Due ${niceDate(d.date)}  ·  ${nudgeDate(d.lastNudged)}", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Canvas); Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!d.settled) TextButton(onClick = nudge) { Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Nudge") }
            TextButton(onClick = toggle) { Text(if (d.settled) "Reopen" else "Mark settled") }
            Spacer(Modifier.weight(1f)); IconButton(onClick = delete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete due", tint = Muted) }
        }
    }
}
@Composable private fun TopBar(title: String, back: () -> Unit) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back") }; Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ink) } }
@Composable private fun Input(value: String, change: (String) -> Unit, label: String, placeholder: String = "", keyboard: KeyboardType = KeyboardType.Text) { OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, placeholder = { Text(placeholder) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard)) }
@Composable private fun AddScreen(onBack: () -> Unit, onSave: (Due) -> Unit) {
    var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var date by remember { mutableStateOf(LocalDate.now().toString()) }; var phone by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TopBar("Add a due", onBack); Text("The little details, all in one place.", color = Muted, modifier = Modifier.padding(start = 8.dp)) }
        item { Input(name, { name = it }, "Who owes you?", "e.g. Ananya") }
        item { Input(amount, { amount = it }, "Amount in ₹", "e.g. 500", KeyboardType.Decimal) }
        item { Input(reason, { reason = it }, "What was it for?", "e.g. Dinner") }
        item { Input(date, { date = it }, "Due date (YYYY-MM-DD)", "2026-12-31") }
        item { Input(phone, { phone = it }, "WhatsApp number (optional)", "Country code + number, e.g. 919876543210", KeyboardType.Phone); Text("Include the country code without + or spaces, or choose a contact in WhatsApp.", color = Muted, fontSize = 13.sp) }
        if (error.isNotEmpty()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        item { Button(onClick = {
            val number = amount.toBigDecimalOrNull(); val validDate = try { LocalDate.parse(date); true } catch (_: Exception) { false }
            error = when { name.isBlank() || reason.isBlank() -> "Add a name and reason."; number == null || number <= BigDecimal.ZERO || number.scale() > 2 || number > BigDecimal("10000000") -> "Enter an amount greater than zero, with up to 2 decimal places."; !validDate -> "Enter a valid date in YYYY-MM-DD format."; phone.isNotBlank() && !phone.matches(Regex("[1-9][0-9]{7,14}")) -> "Enter the WhatsApp number with country code, digits only."; else -> "" }
            if (error.isEmpty()) onSave(Due(name = name.trim(), amount = number!!.setScale(2).toPlainString(), reason = reason.trim(), date = date, phone = phone))
        }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(14.dp)) { Text("Save due", fontWeight = FontWeight.Bold) } }
    }
}
@Composable private fun SettingsScreen(current: String, back: () -> Unit, save: (String) -> Unit) {
    var upi by remember { mutableStateOf(current) }; var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TopBar("Payment settings", back); Text("Where should your friends pay you?", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Add your UPI ID to include a payment link in your reminders.", color = Muted)
        Input(upi, { upi = it }, "Your UPI ID", "yourname@upi")
        Text("Stored only on this device. Double-check the ID before sharing.", color = Muted, fontSize = 14.sp)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = { val v = upi.trim(); if (v.isNotEmpty() && !v.matches(Regex("[A-Za-z0-9._-]+@[A-Za-z0-9._-]+"))) error = "Enter a valid UPI ID, like name@bank." else save(v) }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(14.dp)) { Text("Save settings") }
    }
}
@Composable private fun NudgeScreen(d: Due, upi: String, back: () -> Unit, settings: () -> Unit, onOpened: () -> Unit) {
    val context = LocalContext.current; var tone by remember { mutableStateOf("Friendly") }; var text by remember { mutableStateOf(message(d, upi, tone)) }; var error by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TopBar("Send a little nudge", back); Text("A gentle reminder goes a long way.", color = Muted) }
        item { Text("CHOOSE YOUR TONE", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Friendly", "Playful", "Direct").forEach { option -> FilterChip(selected = tone == option, onClick = { tone = option; text = message(d, upi, option) }, label = { Text(option) }) } } }
        item { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Your message · tap to edit") }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), shape = RoundedCornerShape(16.dp)) }
        if (upi.isBlank()) item { TextButton(onClick = settings) { Text("Add your UPI ID to include a payment link") } }
        item { Text("WhatsApp opens with your message ready. Review and send it yourself.", color = Muted, fontSize = 14.sp) }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        item { Button(onClick = {
            if (text.isBlank()) { error = "Write a message first." } else try {
                val url = "https://wa.me/${d.phone}?text=${Uri.encode(text)}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                onOpened()
            } catch (_: Exception) { error = "Couldn't open WhatsApp. Check that a browser or WhatsApp is installed." }
        }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Send, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Continue to WhatsApp", fontWeight = FontWeight.Bold) } }
    }
}
