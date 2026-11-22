package com.punith.awkwardfree.model

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Split an exact INR amount into positive shares, distributing extra paise in order. */
internal fun splitEqually(total: BigDecimal, people: Int): List<BigDecimal> {
    require(people in 2..50)
    require(total > BigDecimal.ZERO && total <= BigDecimal("10000000") && total.scale() <= 2)
    val paise = total.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toBigIntegerExact()
    require(paise >= BigInteger.valueOf(people.toLong()))
    val parts = paise.divideAndRemainder(BigInteger.valueOf(people.toLong()))
    return (0 until people).map { i ->
        BigDecimal(parts[0] + if (i < parts[1].toInt()) BigInteger.ONE else BigInteger.ZERO, 2)
    }
}
