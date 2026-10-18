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
    val averagePrice: BigDecimal
        get() = paidTao.toBigDecimal()
            .divide(receivedAlpha.toBigDecimal(), 18, RoundingMode.DOWN)

    init {
        require(paidTao.isPositive() && receivedAlpha.isPositive())
    }

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
            if (BittensorNetuid.from(row.netuid).isRoot() || isNonTradeOperation(row.type)) {
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
        val buy = matchesReportedPrice(trade.amountIn, trade.amountOut, trade.price)
        val sell = matchesReportedPrice(trade.amountOut, trade.amountIn, trade.price)
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

    private fun matchesReportedPrice(tao: BigInteger, alpha: BigInteger, price: BigDecimal): Boolean {
        val reportedTao = tao.toBigDecimal()
        val pricedAlpha = alpha.toBigDecimal() * price
        val meetsLowerBound = reportedTao * PRICE_TOLERANCE_FACTOR >= pricedAlpha
        val meetsUpperBound = reportedTao <= pricedAlpha * PRICE_TOLERANCE_FACTOR

        return meetsLowerBound && meetsUpperBound
    }

    private fun isNonTradeOperation(operationType: String?): Boolean {
        if (operationType == null) return false

        return operationTypeWords(operationType)
            .any { it.lowercase() in NON_TRADE_WORDS }
    }

    private fun operationTypeWords(operationType: String): List<String> {
        val separatedWords = operationType
            .replace(CAMEL_CASE_BOUNDARY, "$1 $2")
            .replace(ACRONYM_WORD_BOUNDARY, "$1 $2")

        return separatedWords.split(NON_LETTER_SEPARATOR)
    }

    private companion object {
        // Matches the iOS ledger heuristic: accept ratios from 1 / 1.5 through 1.5.
        val PRICE_TOLERANCE_FACTOR = 1.5.toBigDecimal()

        val NON_TRADE_WORDS = setOf("move", "transfer")

        val CAMEL_CASE_BOUNDARY = Regex("([a-z])([A-Z])")

        val ACRONYM_WORD_BOUNDARY = Regex("([A-Z])([A-Z][a-z])")

        val NON_LETTER_SEPARATOR = Regex("[^A-Za-z]+")
    }
}
