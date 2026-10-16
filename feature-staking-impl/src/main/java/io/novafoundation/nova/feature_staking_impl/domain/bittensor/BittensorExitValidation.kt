package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import java.math.BigInteger

internal fun validateBittensorExit(preflight: BittensorExitPreflight, quote: BittensorExitQuote) {
    require(preflight.rootHoldRemaining.isZero) { "Root stake is still locked" }
    require(quote.amount <= preflight.stake && quote.amount <= preflight.availability.available) { "Amount exceeds available stake" }
    if (quote.fullExit) require(preflight.stake == quote.amount) { "The full position amount has changed. Review the withdrawal again" }
    if (quote.fullExit) require(preflight.stake <= preflight.availability.available) { "Some of this stake is locked" }
    if (!quote.fullExit) {
        require(quote.minimumTaoOut >= preflight.minStake) { "Amount is below the chain's minimum withdrawal" }
        val remainder = preflight.stake - quote.amount
        val remainderTao = if (quote.netuid.isRoot()) remainder else remainder * requireNotNull(quote.limitPrice) / BittensorProtocolUnits.RAO_PER_TAO
        require(remainder == BigInteger.ZERO || preflight.isOwnHotkey || remainderTao >= preflight.minNominatorStake) {
            "The remainder would be swept or locked by the chain. Choose Max to exit the full position"
        }
    }
}
