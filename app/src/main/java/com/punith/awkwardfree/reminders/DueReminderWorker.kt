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
import com.punith.awkwardfree.data.Store
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit

class DueReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = Store(applicationContext)
        if (!store.remindersEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val today = LocalDate.now()
        val count = store.read().count { !it.settled && try { !LocalDate.parse(it.date).isAfter(today) } catch (_: Exception) { false } }
        if (count == 0) return Result.success()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("due_reminders", "Due date reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, "due_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("A little reminder")
            .setContentText("You have $count ${if (count == 1) "tab" else "tabs"} due or overdue. Open the app to review.")
            .setAutoCancel(true).setContentIntent(intent).build()
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
