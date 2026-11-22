package com.punith.awkwardfree.model

/**
 * Returns international digits for WhatsApp and identity matching.
 * Unprefixed 10-digit Indian mobile numbers default to +91.
 * Invalid input is not silently converted into a different recipient.
 */
internal fun normalizePhone(raw: String): String? {
    val input = raw.trim()
    if (input.isEmpty() || input.any { it !in '0'..'9' && it !in " +()-." }) return null
    if (input.count { it == '+' } > 1 || ('+' in input && !input.startsWith("+"))) return null
    val explicit = input.startsWith("+") || input.startsWith("00")
    var digits = input.filter { it in '0'..'9' }
    if (input.startsWith("00")) digits = digits.drop(2)
    if (!explicit && digits.length == 10 && digits[0] in '6'..'9') digits = "91$digits"
    return digits.takeIf { it.matches(Regex("[1-9][0-9]{7,14}")) }
}
