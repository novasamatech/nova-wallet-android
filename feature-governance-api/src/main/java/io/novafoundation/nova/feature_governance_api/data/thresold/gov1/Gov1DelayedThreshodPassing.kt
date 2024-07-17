package io.novafoundation.nova.feature_governance_api.data.thresold.gov1

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DelayedThresholdPassing
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigDecimal

/**
 * This class is using as a placeholder for Gov1 since Gov1 doesn't support delaying
 */
class Gov1DelayedThresholdPassing(
    val threshold: VotingThreshold
) : DelayedThresholdPassing {

    override fun supportPassingInFuture(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): DelayedThresholdPassing.DelayedPassing {
        val supportThreshold = threshold.supportThreshold(tally, totalIssuance, passedSinceDecidingFraction)

        return DelayedThresholdPassing.DelayedPassing(BigDecimal.ONE, supportThreshold.currentlyPassing)
    }

    override fun ayePassingInFuture(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): DelayedThresholdPassing.DelayedPassing {
        val approvalThreshold = threshold.ayesFractionThreshold(tally, totalIssuance, passedSinceDecidingFraction)

        return DelayedThresholdPassing.DelayedPassing(BigDecimal.ONE, approvalThreshold.currentlyPassing)
    }
}
