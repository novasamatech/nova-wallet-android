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
        operations.asReversed().forEach { operation ->
            if (!seen.add(operation)) return@forEach
            when (val classification = classify(operation)) {
                is Classification.Purchase -> addPurchase(operation.netuid, classification.paidTao, classification.receivedAlpha)
                is Classification.Sale -> reducePurchase(operation.netuid, classification.soldAlpha)
                Classification.Unclassified -> unclassified += operation.netuid
                Classification.Ignored -> Unit
            }
        }
    }

    // Provider rows always report TAO in and alpha out; only the event name carries the direction.
    private fun classify(operation: BittensorReportedOperation): Classification {
        val event = TradeEvent.from(operation.eventId)
        val isNonTrade = operationTypeWords(operation.type).any { it in NON_TRADE_WORDS }
        if (BittensorNetuid.from(operation.netuid).isRoot() || event == null || isNonTrade) {
            return Classification.Ignored
        }

        val tao = parseAmount(operation.amountIn)
        val alpha = parseAmount(operation.amountOut)
        val price = operation.price.toBigDecimalOrNull()
            ?.abs()
        if (tao == null || alpha == null || price == null) return Classification.Unclassified
        if (tao.isZero || alpha.isZero || price.isZero) return Classification.Ignored
        if (!matchesReportedPrice(tao, alpha, price)) return Classification.Unclassified

        return when (event) {
            TradeEvent.STAKE_ADDED -> Classification.Purchase(tao, alpha)
            TradeEvent.STAKE_REMOVED -> Classification.Sale(alpha)
        }
    }

    private fun parseAmount(value: String): BigInteger? = value.toBigDecimalOrNull()
        ?.abs()
        ?.movePointRight(BittensorProtocolUnits.ALPHA_PRECISION)
        ?.toBigInteger()

    private fun addPurchase(
        netuid: Int,
        tao: BigInteger,
        alpha: BigInteger
    ) {
        totals[netuid] = totals[netuid]?.adding(tao, alpha)
            ?: BittensorEstimatedPurchaseTotals(tao, alpha)
    }

    private fun reducePurchase(
        netuid: Int,
        alpha: BigInteger
    ) {
        val remaining = totals[netuid]?.reducing(alpha)
        if (remaining == null) totals.remove(netuid) else totals[netuid] = remaining
    }

    private enum class TradeEvent(val eventName: String) {
        STAKE_ADDED("StakeAdded"),
        STAKE_REMOVED("StakeRemoved");

        companion object {
            fun from(eventName: String): TradeEvent? = entries.firstOrNull { it.eventName == eventName }
        }
    }

    private sealed interface Classification {
        class Purchase(val paidTao: BigInteger, val receivedAlpha: BigInteger) : Classification
        class Sale(val soldAlpha: BigInteger) : Classification
        object Ignored : Classification
        object Unclassified : Classification
    }

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
    }
}
