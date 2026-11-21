package com.punith.awkwardfree.util

internal data class VoiceDue(val name: String?, val amount: String?, val reason: String?)

/** Extract only explicit details; the user reviews the resulting form before saving. */
internal fun parseVoiceDue(text: String): VoiceDue {
    val input = text.trim()
    val money = Regex("(?<![\\d.])(?:₹\\s*|(?:rs\\.?|rupees?)\\s*)?(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)(?![\\d.])", RegexOption.IGNORE_CASE).find(input)
    val amount = money?.groupValues?.get(1)?.replace(",", "")
    val namePatterns = listOf(
        Regex("^\\s*(.+?)\\s+(?:borrowed|owes|took)\\b", RegexOption.IGNORE_CASE),
        Regex("^\\s*I\\s+(?:gave|lent|loaned)\\s+(.+?)\\s+(?:(?:₹|rs\\.?|rupees?)\\s*)?\\d", RegexOption.IGNORE_CASE)
    )
    val name = namePatterns.firstNotNullOfOrNull { it.find(input)?.groupValues?.get(1)?.trim() }
        ?.takeIf { it.isNotBlank() && it.length <= 60 }
    val reason = Regex("\\bfor\\s+(.+)$", RegexOption.IGNORE_CASE).find(input)?.groupValues?.get(1)
        ?.trim()?.trimEnd('.', '!', '?')?.takeIf { it.isNotBlank() }
    return VoiceDue(name, amount, reason)
}
