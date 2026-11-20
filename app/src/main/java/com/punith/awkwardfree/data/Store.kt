package com.punith.awkwardfree.data

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.punith.awkwardfree.model.*
import org.json.JSONArray
import org.json.JSONObject

internal class Store(activity: ComponentActivity) {
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
