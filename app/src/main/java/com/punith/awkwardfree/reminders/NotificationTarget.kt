package com.punith.awkwardfree.reminders

internal data class NotificationTarget(val dueId: String, val ownerId: String, val openNudge: Boolean)

internal const val ACTION_REVIEW_DUE = "com.punith.awkwardfree.action.REVIEW_DUE"
internal const val EXTRA_DUE_ID = "due_id"
internal const val EXTRA_OWNER_ID = "owner_id"
internal const val EXTRA_OPEN_NUDGE = "open_nudge"
