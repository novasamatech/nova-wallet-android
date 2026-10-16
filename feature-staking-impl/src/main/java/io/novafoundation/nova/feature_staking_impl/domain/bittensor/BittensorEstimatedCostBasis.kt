package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.common.utils.isZero
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class BittensorReportedOperation(
    val netuid: Int,
    val hotkey: String,
    val amountIn: String,
    val amountOut: String,
    val price: String,
    val eventId: String,
    val timestamp: String,
    val reference: String,
    val type: String?
)

data class BittensorEstimatedPurchaseTotals(val paidTao: BigInteger, val receivedAlpha: BigInteger) {
    init { require(paidTao.isPositive() && receivedAlpha.isPositive()) }

    val averagePrice: BigDecimal
        get() = paidTao.toBigDecimal().divide(receivedAlpha.toBigDecimal(), 18, RoundingMode.DOWN)

    fun adding(tao: BigInteger, alpha: BigInteger) = BittensorEstimatedPurchaseTotals(paidTao + tao, receivedAlpha + alpha)

    fun reducing(alpha: BigInteger): BittensorEstimatedPurchaseTotals? {
        if (alpha >= receivedAlpha) return null
        val remaining = receivedAlpha - alpha
        val tao = remaining * paidTao / receivedAlpha
        return if (tao.isPositive()) BittensorEstimatedPurchaseTotals(tao, remaining) else null
    }

    fun estimatedProfit(soldAlpha: BigInteger, taoOut: BigInteger): BigInteger = taoOut - soldAlpha * paidTao / receivedAlpha
}

/** Operator-approved parity with the iOS ledger, not an authoritative accounting history. */
class BittensorEstimatedCostBasis(operations: List<BittensorReportedOperation>, private val allProviderPagesRead: Boolean) {
    private val totals = mutableMapOf<Int, BittensorEstimatedPurchaseTotals>()

    private val unclassified = mutableSetOf<Int>()

    init {
        val seen = mutableSetOf<BittensorReportedOperation>()
        operations.asReversed().forEach { row ->
            if (row.eventId.isNotEmpty() && !seen.add(row)) return@forEach
            if (row.netuid == 0 || nonTrade(row.type)) return@forEach
            val amountIn = row.amountIn.toBigDecimalOrNull()?.abs()?.movePointRight(BittensorProtocolUnits.ALPHA_PRECISION)?.toBigInteger()
            val amountOut = row.amountOut.toBigDecimalOrNull()?.abs()?.movePointRight(BittensorProtocolUnits.ALPHA_PRECISION)?.toBigInteger()
            val price = row.price.toBigDecimalOrNull()?.abs()
            if (amountIn == null || amountOut == null || price == null) {
                unclassified += row.netuid
                return@forEach
            }
            if (amountIn.isZero || amountOut.isZero || price.isZero) return@forEach
            val buy = fits(amountIn, amountOut, price)
            val sell = fits(amountOut, amountIn, price)
            when {
                buy && !sell -> totals[row.netuid] = totals[row.netuid]?.adding(amountIn, amountOut)
                    ?: BittensorEstimatedPurchaseTotals(amountIn, amountOut)
                sell && !buy -> totals[row.netuid]?.reducing(amountIn)?.let { totals[row.netuid] = it } ?: totals.remove(row.netuid)
                else -> unclassified += row.netuid
            }
        }
    }

    fun canEstimate(netuid: Int): Boolean = allProviderPagesRead && netuid !in unclassified

    fun forSubnet(netuid: Int): BittensorEstimatedPurchaseTotals? {
        if (!allProviderPagesRead || netuid in unclassified) return null
        return totals[netuid]
    }

    private fun fits(tao: BigInteger, alpha: BigInteger, price: BigDecimal): Boolean {
        val taoValue = tao.toBigDecimal()
        val alphaValue = alpha.toBigDecimal() * price
        return taoValue * BigDecimal(3) >= alphaValue * BigDecimal(2) && taoValue * BigDecimal(2) <= alphaValue * BigDecimal(3)
    }

    private fun nonTrade(label: String?): Boolean {
        val separated = label?.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            ?.replace(Regex("([A-Z])([A-Z][a-z])"), "$1 $2") ?: return false
        return separated.split(Regex("[^A-Za-z]+")).any { it.equals("move", true) || it.equals("transfer", true) }
    }
}
