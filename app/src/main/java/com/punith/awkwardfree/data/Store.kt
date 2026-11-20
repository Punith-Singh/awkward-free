package com.punith.awkwardfree.data

import android.content.Context
import com.punith.awkwardfree.model.Due
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

internal data class Backup(val dues: List<Due>, val upi: String, val displayName: String)

internal class Store(context: Context, userId: String? = null) {
    private val prefs = context.getSharedPreferences(if (userId == null) "awkward_free" else "awkward_free_user_$userId", 0)
    var upi: String
        get() = prefs.getString("upi", "") ?: ""
        set(value) { prefs.edit().putString("upi", value).apply() }
    var displayName: String
        get() = prefs.getString("display_name", "") ?: ""
        set(value) { prefs.edit().putString("display_name", value).apply() }
    var remindersEnabled: Boolean
        get() = prefs.getBoolean("reminders_enabled", false)
        set(value) { prefs.edit().putBoolean("reminders_enabled", value).apply() }
    fun read(): List<Due> = try { parseDues(JSONArray(prefs.getString("dues", "[]"))) } catch (_: Exception) { emptyList() }
    fun write(dues: List<Due>) { prefs.edit().putString("dues", encodeDues(dues).toString()).apply() }
    fun exportBackup(): String = JSONObject().put("version", 1).put("dues", encodeDues(read()))
        .put("upi", upi).put("displayName", displayName).toString(2)
    fun parseBackup(text: String): Backup {
        require(text.length <= 2_000_000) { "Backup file is too large." }
        val json = JSONObject(text)
        require(json.getInt("version") == 1) { "This backup version isn't supported." }
        val entries = json.getJSONArray("dues")
        require(entries.length() <= 10000) { "Too many records." }
        val items = parseDues(entries)
        require(items.map { it.id }.toSet().size == items.size) { "Duplicate records in backup." }
        val restoredUpi = json.getString("upi")
        val restoredName = json.getString("displayName")
        require(restoredUpi.length <= 200 && restoredName.length <= 200) { "Invalid settings." }
        return Backup(items, restoredUpi, restoredName)
    }
    fun restore(backup: Backup): Boolean = prefs.edit().putString("dues", encodeDues(backup.dues).toString())
        .putString("upi", backup.upi).putString("display_name", backup.displayName).commit()
    private fun encodeDues(dues: List<Due>): JSONArray = JSONArray().also { array -> dues.forEach { d ->
        array.put(JSONObject().put("id", d.id).put("name", d.name).put("amount", d.amount).put("reason", d.reason)
            .put("date", d.date).put("phone", d.phone).put("settled", d.settled).put("lastNudged", d.lastNudged).put("paidAmount", d.paidAmount))
    } }
    private fun parseDues(data: JSONArray): List<Due> = (0 until data.length()).map { i -> data.getJSONObject(i).let { o ->
        val amount = o.getString("amount").toBigDecimal(); val paid = o.optString("paidAmount", "0.00").toBigDecimal()
        require(amount > BigDecimal.ZERO && amount.scale() <= 2 && amount <= BigDecimal("10000000") && paid >= BigDecimal.ZERO && paid.scale() <= 2 && paid <= amount) { "Invalid amount." }
        val id = o.getString("id"); val name = o.getString("name"); val reason = o.getString("reason"); val date = o.getString("date"); val phone = o.optString("phone")
        require(id.isNotBlank() && name.isNotBlank() && reason.isNotBlank() && id.length <= 200 && name.length <= 500 && reason.length <= 1000 && phone.length <= 15) { "Invalid due." }
        LocalDate.parse(date)
        Due(id, name, amount.toPlainString(), reason, date, phone, o.optBoolean("settled"), o.optLong("lastNudged"), paid.toPlainString())
    } }
}
