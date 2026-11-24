package com.punith.awkwardfree.reminders

import java.time.ZonedDateTime

/** Quiet hours run from 10 PM to 8 AM, in the device's local time zone. */
internal fun outsideQuietHours(time: ZonedDateTime, enabled: Boolean): ZonedDateTime {
    if (!enabled) return time
    return when {
        time.hour >= 22 -> time.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0)
        time.hour < 8 -> time.withHour(8).withMinute(0).withSecond(0).withNano(0)
        else -> time
    }
}

internal fun suggestedFollowUpTone(count: Int): String = when {
    count <= 0 -> "Friendly"
    count == 1 -> "Follow-up"
    count == 2 -> "Polite follow-up"
    else -> "Direct"
}
