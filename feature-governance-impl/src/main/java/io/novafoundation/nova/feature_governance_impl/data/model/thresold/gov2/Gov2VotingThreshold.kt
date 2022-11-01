package io.novafoundation.nova.feature_governance_impl.data.model.thresold.gov2

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold.Threshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeVotes
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class Gov2VotingThreshold(
    val supportCurve: VotingCurve,
    val approvalCurve: VotingCurve
) : VotingThreshold {

    override fun supportThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Balance> {
        val supportNeeded = supportCurve.threshold(passedSinceDecidingFraction) * totalIssuance.toBigDecimal()
        val supportNeededIntegral = supportNeeded.toBigInteger()

        return Threshold(
            value = supportNeededIntegral,
            passing = tally.support >= supportNeededIntegral
        )
    }

    override fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill> {
        val approvalThreshold = approvalCurve.threshold(passedSinceDecidingFraction)
        val ayeFraction = tally.ayeVotes().fraction

        return Threshold(
            value = approvalThreshold,
            passing = ayeFraction >= approvalThreshold
        )
    }
}

fun Gov2VotingThreshold(trackInfo: TrackInfo): Gov2VotingThreshold {
    return Gov2VotingThreshold(
        supportCurve = trackInfo.minSupport!!,
        approvalCurve = trackInfo.minApproval!!
    )
}
