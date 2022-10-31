package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface VotingThreshold {

    fun supportNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Balance

    fun ayesFractionNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Perbill
}
