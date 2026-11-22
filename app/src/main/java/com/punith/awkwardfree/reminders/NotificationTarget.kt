package com.punith.awkwardfree.reminders

import com.punith.awkwardfree.model.Due

internal data class NotificationTarget(val dueId: String, val ownerId: String, val openNudge: Boolean)

internal const val ACTION_REVIEW_DUE = "com.punith.awkwardfree.action.REVIEW_DUE"
internal const val EXTRA_DUE_ID = "due_id"
internal const val EXTRA_OWNER_ID = "owner_id"
internal const val EXTRA_OPEN_NUDGE = "open_nudge"

internal fun resolveNotificationTarget(target: NotificationTarget, owner: String, dues: List<Due>): Pair<String, String>? {
    if (target.ownerId != owner) return null
    val due = dues.firstOrNull { it.id == target.dueId && !it.settled } ?: return null
    return due.id to if (target.openNudge && !due.owedByMe) "nudge" else "details"
}
