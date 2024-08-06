package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigDecimal

interface VotingThreshold {

    class Threshold<T>(val value: T, val currentlyPassing: Boolean, val projectedPassing: ProjectedPassing) {
        companion object;
    }

    class ProjectedPassing(val delayFraction: Perbill, val passingInFuture: Boolean)

    fun supportThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Balance>

    fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill>
}

fun VotingThreshold.Threshold.Companion.simple(value: BigDecimal, currentlyPassing: Boolean) =
    VotingThreshold.Threshold<BigDecimal>(value, currentlyPassing = true, VotingThreshold.ProjectedPassing(value, currentlyPassing))

fun <T> VotingThreshold.Threshold.Companion.passing(value: T) =
    VotingThreshold.Threshold(value, currentlyPassing = true, VotingThreshold.ProjectedPassing(BigDecimal.ZERO, passingInFuture = true))

fun <T> VotingThreshold.Threshold.Companion.notPassing(value: T) =
    VotingThreshold.Threshold(value, currentlyPassing = false, VotingThreshold.ProjectedPassing(BigDecimal.ONE, passingInFuture = false))

fun VotingThreshold.ProjectedPassing.merge(another: VotingThreshold.ProjectedPassing): VotingThreshold.ProjectedPassing {
    return VotingThreshold.ProjectedPassing(
        delayFraction = delayFraction.coerceAtLeast(another.delayFraction),
        passingInFuture = passingInFuture && another.passingInFuture
    )
}
