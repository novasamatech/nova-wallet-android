package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote

/**
 * The quote the sale was acknowledged at, its latest block re-quote and whether the price
 * moved past the acknowledged limit since. The review shows the latest quote and falls back to the acknowledged one.
 */
internal data class BittensorExitReviewQuotes(
    val acknowledged: BittensorExitQuote,
    val latest: BittensorExitQuote? = null,
    val isPriceMoved: Boolean = false
) {

    val displayed: BittensorExitQuote
        get() = latest ?: acknowledged

    val latestCrossesAcknowledgedLimit: Boolean
        get() = latest?.let(::crossesAcknowledgedLimit) ?: false

    /** A re-quote past the acknowledged limit raises the price move until it is acknowledged. */
    fun withLatest(quote: BittensorExitQuote) = copy(latest = quote, isPriceMoved = isPriceMoved || crossesAcknowledgedLimit(quote))

    /** The quote re-read on Confirm did not fill at the acknowledged limit. */
    fun withMovedPrice(quote: BittensorExitQuote) = copy(latest = quote, isPriceMoved = true)

    /** Confirming at the new rate takes the latest quote. */
    fun acknowledgeLatest() = copy(acknowledged = latest ?: acknowledged, isPriceMoved = false)

    /** A failed re-quote drops the latest quote and the price move. */
    fun withoutLatest() = copy(latest = null, isPriceMoved = false)

    private fun crossesAcknowledgedLimit(quote: BittensorExitQuote): Boolean {
        return acknowledged.limitPrice?.let { !quote.isFillable(atLimit = it) } ?: false
    }
}

/**
 * The price-move banner: one validation report per banner, by how it ended. Confirming at the
 * new rate acknowledges it; the hand-off then reports it proceeded, while a new banner, another tap or leaving reports it blocked.
 */
internal class BittensorExitPriceMoveBanner(private val report: (StakingValidationOutcome) -> Unit) {

    private enum class State { SHOWN, ACKNOWLEDGED }

    private var state: State? = null

    fun sync(isShown: Boolean) {
        when {
            isShown && state == null -> state = State.SHOWN
            isShown && state == State.ACKNOWLEDGED -> {
                close(StakingValidationOutcome.BLOCKED)
                state = State.SHOWN
            }
            !isShown && state == State.SHOWN -> close(StakingValidationOutcome.CANCELLED)
        }
    }

    fun beginTap() {
        if (state == State.ACKNOWLEDGED) close(StakingValidationOutcome.BLOCKED)
    }

    fun acknowledge() {
        state = State.ACKNOWLEDGED
    }

    fun takeAcknowledgement() {
        if (state == State.ACKNOWLEDGED) close(StakingValidationOutcome.PROCEEDED)
    }

    fun leave() {
        when (state) {
            State.SHOWN -> close(StakingValidationOutcome.CANCELLED)
            State.ACKNOWLEDGED -> close(StakingValidationOutcome.BLOCKED)
            null -> Unit
        }
    }

    private fun close(outcome: StakingValidationOutcome) {
        state = null
        report(outcome)
    }
}
