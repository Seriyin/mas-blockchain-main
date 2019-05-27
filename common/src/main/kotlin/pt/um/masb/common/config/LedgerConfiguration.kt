package pt.um.masb.common.config

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

object LedgerConfiguration {
    val GLOBALCONTEXT =
        MathContext(12, RoundingMode.HALF_EVEN)
    const val CACHE_SIZE = 40L
    val RECALC_DIV =
        BigInteger("10000000000000")
    val RECALC_MULT =
        BigDecimal("10000000000000")
    const val OTHER_BASE = 40L
    const val DATA_BASE = 5L
}