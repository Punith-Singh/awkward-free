package com.punith.awkwardfree.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.punith.awkwardfree.MainActivity
import com.punith.awkwardfree.R
import com.punith.awkwardfree.data.Store
import com.punith.awkwardfree.util.money
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val KEY_DUE = "due"
private const val KEY_OWNER = "owner"
private const val KEY_NUDGED = "nudged"

private fun enqueueFollowUp(context: Context, owner: String, dueId: String, lastNudged: Long, whenToRun: ZonedDateTime, policy: ExistingWorkPolicy) {
    val delay = Duration.between(ZonedDateTime.now(), whenToRun).toMillis().coerceAtLeast(0L)
    val request = OneTimeWorkRequestBuilder<FollowUpWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(KEY_DUE to dueId, KEY_OWNER to owner, KEY_NUDGED to lastNudged))
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork("follow-up:$owner:$dueId", policy, request)
}

internal fun scheduleFollowUp(context: Context, owner: String, dueId: String, lastNudged: Long) {
    val store = Store(context, owner.ifEmpty { null })
    val whenToRun = outsideQuietHours(ZonedDateTime.now().plusDays(3), store.quietHoursEnabled)
    enqueueFollowUp(context, owner, dueId, lastNudged, whenToRun, ExistingWorkPolicy.REPLACE)
}

class FollowUpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val owner = inputData.getString(KEY_OWNER) ?: return Result.failure()
        val dueId = inputData.getString(KEY_DUE) ?: return Result.failure()
        val stamp = inputData.getLong(KEY_NUDGED, 0L)
        val active = if (FirebaseApp.getApps(applicationContext).isEmpty()) "" else FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (active != owner) return Result.success()
        val store = Store(applicationContext, owner.ifEmpty { null })
        if (!store.remindersEnabled) return Result.success()
        val due = store.read().firstOrNull { it.id == dueId && !it.settled && !it.owedByMe && it.lastNudged == stamp && stamp > 0 } ?: return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val now = ZonedDateTime.now()
        val allowed = outsideQuietHours(now, store.quietHoursEnabled)
        if (allowed.isAfter(now)) {
            enqueueFollowUp(applicationContext, owner, dueId, stamp, allowed, ExistingWorkPolicy.APPEND_OR_REPLACE)
            return Result.success()
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("follow_ups", "Follow-up reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setAction(ACTION_REVIEW_DUE)
            .setData(Uri.Builder().scheme("awkward-free").authority("follow-up").appendPath(owner.ifEmpty { "guest" }).appendPath(dueId).build())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_DUE_ID, dueId).putExtra(EXTRA_OWNER_ID, owner).putExtra(EXTRA_OPEN_NUDGE, true)
        val pending = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, "follow_ups")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for a follow-up?")
            .setContentText("${due.name} · ${money(due.remaining().toPlainString())}. Tap to review your message.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pending).build()
        try { NotificationManagerCompat.from(applicationContext).notify(("follow-up:$owner:$dueId").hashCode(), notification) } catch (_: SecurityException) { }
        return Result.success()
    }
}
