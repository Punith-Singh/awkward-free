package com.punith.awkwardfree.model

import java.math.BigDecimal
import java.util.UUID

internal data class Due(
    val id: String = UUID.randomUUID().toString(), val name: String, val amount: String,
    val reason: String, val date: String, val phone: String = "", val settled: Boolean = false,
    val lastNudged: Long = 0L, val paidAmount: String = "0.00", val owedByMe: Boolean = false,
    val nudgeHistory: List<Long> = emptyList()
) {
    fun recordNudgeOpened(at: Long): Due {
        require(at > 0 && !settled && !owedByMe)
        val previous = if (nudgeHistory.isEmpty() && lastNudged > 0) listOf(lastNudged) else nudgeHistory
        return copy(lastNudged = at, nudgeHistory = (previous + at).takeLast(100))
    }

    fun recordPayment(value: BigDecimal): Due {
        require(!settled && value > BigDecimal.ZERO && value.scale() <= 2 && value <= remaining())
        val paid = ((paidAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) + value).setScale(2)
        return copy(paidAmount = paid.toPlainString(), settled = paid.compareTo(amount.toBigDecimal()) >= 0)
    }
    fun remaining(): BigDecimal = ((amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) -
        (paidAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO)).max(BigDecimal.ZERO)
}
