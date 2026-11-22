package com.punith.awkwardfree.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Returns exact paise shares. Percentage rounding distributes spare paise by remainder. */
internal fun splitShares(total: BigDecimal, mode: String, inputs: List<String>): List<BigDecimal> {
    require(total > BigDecimal.ZERO && total.scale() <= 2 && inputs.isNotEmpty())
    if (mode == "Equal") return splitEqually(total, inputs.size)
    val values = inputs.map { it.toBigDecimalOrNull() ?: throw IllegalArgumentException("Enter every share.") }
    require(values.all { it > BigDecimal.ZERO && it.scale() <= 2 }) { "Shares must be positive, with at most two decimal places." }
    if (mode == "Amount") {
        require(values.fold(BigDecimal.ZERO, BigDecimal::add).compareTo(total) == 0) { "Amounts must add up to the total." }
        return values.map { it.setScale(2) }
    }
    require(mode == "Percent" && values.fold(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal("100")) == 0) { "Percentages must add up to 100." }
    val exact = values.map { total.multiply(it).divide(BigDecimal("100")) }
    val shares = exact.map { it.setScale(2, RoundingMode.DOWN) }.toMutableList()
    val remainingPaise = total.subtract(shares.fold(BigDecimal.ZERO, BigDecimal::add)).movePointRight(2).intValueExact()
    val order = exact.indices.sortedWith(compareByDescending<Int> { exact[it].subtract(shares[it]) }.thenBy { it })
    repeat(remainingPaise) { i -> shares[order[i]] = shares[order[i]].add(BigDecimal("0.01")) }
    require(shares.all { it > BigDecimal.ZERO }) { "Each share must be at least one paisa." }
    return shares
}
