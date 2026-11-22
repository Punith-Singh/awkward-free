package com.punith.awkwardfree.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.punith.awkwardfree.MainActivity
import com.punith.awkwardfree.R
import com.punith.awkwardfree.data.Store
import com.punith.awkwardfree.model.materializeMonthly
import com.punith.awkwardfree.util.money
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit

class DueReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val uid = if (FirebaseApp.getApps(applicationContext).isEmpty()) null else FirebaseAuth.getInstance().currentUser?.uid
        val store = Store(applicationContext, uid)
        if (!store.remindersEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val today = LocalDate.now()
        val upcoming = materializeMonthly(store.read(), today).filter { !it.settled && try { !LocalDate.parse(it.date).isAfter(today.plusDays(1)) } catch (_: Exception) { false } }.sortedBy { it.date }
        val count = upcoming.size
        if (count == 0) return Result.success()
        val lines = upcoming.take(5).map { due -> (if (due.owedByMe) "You owe " else "Owed by ") + due.name + ": " + money(due.remaining().toPlainString()) }
        val inbox = NotificationCompat.InboxStyle().setBigContentTitle("Tabs due soon or overdue")
        lines.forEach { inbox.addLine(it) }
        if (count > lines.size) inbox.setSummaryText("And ${count - lines.size} more")
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("due_reminders", "Due date reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(applicationContext, "due_reminders")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("A little reminder")
            .setContentText(if (count == 1) lines.first() else "You have $count tabs due soon or overdue. Open to review.")
            .setStyle(inbox).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true).setContentIntent(intent)
        if (upcoming.size == 1) {
            val due = upcoming.first()
            fun reviewIntent(openNudge: Boolean, requestCode: Int): PendingIntent {
                val action = Intent(applicationContext, MainActivity::class.java)
                    .setAction(ACTION_REVIEW_DUE)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_DUE_ID, due.id)
                    .putExtra(EXTRA_OWNER_ID, uid ?: "")
                    .putExtra(EXTRA_OPEN_NUDGE, openNudge)
                return PendingIntent.getActivity(applicationContext, requestCode, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            if (!due.owedByMe) builder.addAction(0, "Remind", reviewIntent(true, 1002))
            builder.addAction(0, "Review payment", reviewIntent(false, 1003))
        }
        val notification = builder.build()
        try { NotificationManagerCompat.from(applicationContext).notify(1001, notification) } catch (_: SecurityException) { }
        return Result.success()
    }
}
internal fun scheduleReminders(context: Context, enabled: Boolean) {
    val work = WorkManager.getInstance(context)
    if (!enabled) { work.cancelUniqueWork("due_reminders"); return }
    val now = ZonedDateTime.now(); var next = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
    val request = PeriodicWorkRequestBuilder<DueReminderWorker>(24, TimeUnit.HOURS).setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
    work.enqueueUniquePeriodicWork("due_reminders", ExistingPeriodicWorkPolicy.KEEP, request)
}
