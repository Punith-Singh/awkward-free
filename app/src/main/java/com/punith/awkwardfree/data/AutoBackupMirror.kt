package com.punith.awkwardfree.data

import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Only explicit app data is mirrored. Firebase and other authentication
 * preferences are never copied into the backup directory.
 */
internal class AutoBackupMirror(private val context: Context) {
    private val directory = File(context.filesDir, "auto_backup")
    private fun fileFor(name: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(name.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(directory, "$hash.json")
    }

    fun restoreIfAbsent(name: String, prefs: SharedPreferences, parse: (String) -> Backup) {
        if (prefs.all.isNotEmpty()) return
        val file = fileFor(name)
        if (!file.exists()) return
        runCatching {
            val text = file.readText(Charsets.UTF_8)
            val backup = parse(text)
            val json = JSONObject(text)
            check(prefs.edit().putString("dues", json.getJSONArray("dues").toString())
                .putString("upi", backup.upi).putString("display_name", backup.displayName)
                .putBoolean("reminders_enabled", json.optBoolean("remindersEnabled", false))
                .putBoolean("quiet_hours", json.optBoolean("quietHoursEnabled", true)).commit())
        }
    }

    fun snapshot(name: String, prefs: SharedPreferences, validate: (String) -> Backup): Boolean =
        runCatching {
            val json = JSONObject().put("version", 5)
                .put("dues", JSONArray(prefs.getString("dues", "[]")))
                .put("upi", prefs.getString("upi", "") ?: "")
                .put("displayName", prefs.getString("display_name", "") ?: "")
                .put("remindersEnabled", prefs.getBoolean("reminders_enabled", false))
                .put("quietHoursEnabled", prefs.getBoolean("quiet_hours", true))
            val text = json.toString()
            validate(text) // Never replace a good snapshot with unreadable or oversized data.
            check(directory.exists() || directory.mkdirs())
            val atomic = AtomicFile(fileFor(name))
            val stream = atomic.startWrite()
            try {
                stream.write(text.toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (error: Exception) {
                atomic.failWrite(stream)
                throw error
            }
            BackupManager(context).dataChanged()
        }.isSuccess

    fun snapshotExistingAccounts(validate: (String) -> Backup): Boolean {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val accounts = prefsDir.listFiles()?.filter { it.isFile && it.name.endsWith(".xml") }?.map { it.name.removeSuffix(".xml") }?.filter { it == "awkward_free" || it.startsWith("awkward_free_user_") } ?: emptyList()
        return accounts.all { snapshot(it, context.getSharedPreferences(it, Context.MODE_PRIVATE), validate) }
    }
}
