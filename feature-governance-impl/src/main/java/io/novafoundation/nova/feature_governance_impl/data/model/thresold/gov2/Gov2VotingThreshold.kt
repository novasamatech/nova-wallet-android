package io.novafoundation.nova.feature_governance_impl.data.model.thresold.gov2

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class Gov2VotingThreshold(
    val supportCurve: VotingCurve,
    val approvalCurve: VotingCurve
) : VotingThreshold {

    override fun supportNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Balance {
        val supportNeeded = supportCurve.threshold(passedSinceDecidingFraction) * totalIssuance.toBigDecimal()

        return supportNeeded.toBigInteger()
    }

    override fun ayesFractionNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Perbill {
        return approvalCurve.threshold(passedSinceDecidingFraction)
    }
}

fun Gov2VotingThreshold(trackInfo: TrackInfo): Gov2VotingThreshold {
    return Gov2VotingThreshold(
        supportCurve = trackInfo.minSupport,
        approvalCurve = trackInfo.minApproval
    )
}
