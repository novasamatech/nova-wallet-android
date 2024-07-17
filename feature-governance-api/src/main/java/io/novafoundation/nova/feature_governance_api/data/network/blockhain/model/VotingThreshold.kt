package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface VotingThreshold {

    class Threshold<T>(val value: T, val currentlyPassing: Boolean) {
        companion object;
    }

    fun supportThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Balance>

    fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill>
}

fun <T> VotingThreshold.Threshold.Companion.passing(value: T) =
    VotingThreshold.Threshold(value, currentlyPassing = true)

fun <T> VotingThreshold.Threshold.Companion.notPassing(value: T) =
    VotingThreshold.Threshold(value, currentlyPassing = false)
