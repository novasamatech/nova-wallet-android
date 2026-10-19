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
            val operationWords = operationTypeWords(row.type)
            val isNonTrade = operationWords.any { it in NON_TRADE_WORDS }
            if (BittensorNetuid.from(row.netuid).isRoot() || isNonTrade) {
                return@forEach
            }
            applyOperation(row, operationWords)
        }
    }

    private fun applyOperation(operation: BittensorReportedOperation, operationWords: Set<String>) {
        val trade = parseTrade(operation)
        if (trade == null) {
            unclassified += operation.netuid
            return
        }
        if (trade.amountIn.isZero || trade.amountOut.isZero || trade.price.isZero) return
        val fitsPurchase = matchesReportedPrice(trade.amountIn, trade.amountOut, trade.price)
        val fitsSale = matchesReportedPrice(trade.amountOut, trade.amountIn, trade.price)
        val hasPurchaseLabel = operationWords.any { it in PURCHASE_WORDS }
        val hasSaleLabel = operationWords.any { it in SALE_WORDS }
        val buy = fitsPurchase && (!fitsSale || hasPurchaseLabel)
        val sell = fitsSale && (!fitsPurchase || hasSaleLabel)
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

    private fun operationTypeWords(operationType: String?): Set<String> = buildSet {
        if (operationType == null) return@buildSet

        var wordStart = 0
        for (index in operationType.indices) {
            if (!operationType[index].isLetter()) {
                if (index > wordStart) add(operationType.substring(wordStart, index).lowercase())
                wordStart = index + 1
            } else if (index > wordStart && startsCamelCaseWord(operationType, index)) {
                add(operationType.substring(wordStart, index).lowercase())
                wordStart = index
            }
        }
        if (wordStart < operationType.length) add(operationType.substring(wordStart).lowercase())
    }

    private fun startsCamelCaseWord(value: String, index: Int): Boolean {
        val previous = value[index - 1]
        val precedesLowercase = value.getOrNull(index + 1)?.isLowerCase() == true
        return value[index].isUpperCase() && (previous.isLowerCase() || previous.isUpperCase() && precedesLowercase)
    }

    private companion object {
        // Matches the iOS ledger heuristic: accept ratios from 1 / 1.5 through 1.5.
        val PRICE_TOLERANCE_FACTOR = 1.5.toBigDecimal()

        val NON_TRADE_WORDS = setOf("move", "moved", "transfer", "transferred")

        val PURCHASE_WORDS = setOf("add", "added", "buy", "bought")

        val SALE_WORDS = setOf("remove", "removed", "unstake", "sell", "sold")
    }
}
