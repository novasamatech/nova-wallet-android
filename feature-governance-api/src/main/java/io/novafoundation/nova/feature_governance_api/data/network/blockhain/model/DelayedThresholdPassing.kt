package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DelayedThresholdPassing.*
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface DelayedThresholdPassing {

    class DelayedPassing(val delayFraction: Perbill, val passingInFuture: Boolean)

    fun supportPassingInFuture(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): DelayedPassing

    fun ayePassingInFuture(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): DelayedPassing
}

fun DelayedPassing.merge(another: DelayedPassing): DelayedPassing {
    return DelayedPassing(
        delayFraction = delayFraction.coerceAtLeast(another.delayFraction),
        passingInFuture = passingInFuture && another.passingInFuture
    )
}
