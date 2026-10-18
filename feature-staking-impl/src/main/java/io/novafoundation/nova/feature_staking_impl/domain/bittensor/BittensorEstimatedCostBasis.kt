package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isZero
import io.novasama.substrate_sdk_android.hash.isPositive
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

data class BittensorEstimatedPurchaseTotals(
    val paidTao: BigInteger,
    val receivedAlpha: BigInteger
) {
    init {
        require(paidTao.isPositive() && receivedAlpha.isPositive())
    }

    val averagePrice: BigDecimal
        get() = paidTao.toBigDecimal()
            .divide(receivedAlpha.toBigDecimal(), 18, RoundingMode.DOWN)

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
class BittensorEstimatedCostBasis(
    operations: List<BittensorReportedOperation>,
    private val allProviderPagesRead: Boolean
) {

    private val totals = mutableMapOf<Int, BittensorEstimatedPurchaseTotals>()

    private val unclassified = mutableSetOf<Int>()

    init {
        val seen = mutableSetOf<BittensorReportedOperation>()
        operations.asReversed().forEach { row ->
            if (row.eventId.isNotEmpty() && !seen.add(row)) return@forEach
            if (BittensorNetuid.from(row.netuid).isRoot() || nonTrade(row.type)) {
                return@forEach
            }
            applyOperation(row)
        }
    }

    private fun applyOperation(operation: BittensorReportedOperation) {
        val trade = parseTrade(operation)
        if (trade == null) {
            unclassified += operation.netuid
            return
        }
        if (trade.amountIn.isZero || trade.amountOut.isZero || trade.price.isZero) return
        val buy = fits(trade.amountIn, trade.amountOut, trade.price)
        val sell = fits(trade.amountOut, trade.amountIn, trade.price)
        when {
            buy && !sell -> addPurchase(operation.netuid, trade)
            sell && !buy -> reducePurchase(operation.netuid, trade.amountIn)
            else -> unclassified += operation.netuid
        }
    }

    private fun parseTrade(operation: BittensorReportedOperation): ReportedTrade? {
        val amountIn = parseAmount(operation.amountIn) ?: return null
        val amountOut = parseAmount(operation.amountOut) ?: return null
        val price = operation.price.toBigDecimalOrNull()
            ?.abs() ?: return null
        return ReportedTrade(amountIn, amountOut, price)
    }

    private fun parseAmount(value: String): BigInteger? = value.toBigDecimalOrNull()
        ?.abs()
        ?.movePointRight(BittensorProtocolUnits.ALPHA_PRECISION)
        ?.toBigInteger()

    private fun addPurchase(
        netuid: Int,
        trade: ReportedTrade
    ) {
        totals[netuid] = totals[netuid]?.adding(trade.amountIn, trade.amountOut)
            ?: BittensorEstimatedPurchaseTotals(trade.amountIn, trade.amountOut)
    }

    private fun reducePurchase(
        netuid: Int,
        alpha: BigInteger
    ) {
        val remaining = totals[netuid]?.reducing(alpha)
        if (remaining == null) totals.remove(netuid) else totals[netuid] = remaining
    }

    private data class ReportedTrade(val amountIn: BigInteger, val amountOut: BigInteger, val price: BigDecimal)

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
