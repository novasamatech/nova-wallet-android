package io.novafoundation.nova.feature_governance_api.data.thresold.gov2

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.divideOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold.Threshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeVotes
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.MathContext

class Gov2VotingThreshold(
    val supportCurve: VotingCurve,
    val approvalCurve: VotingCurve
) : VotingThreshold {

    override fun supportThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Balance> {
        val supportNeeded = supportCurve.threshold(passedSinceDecidingFraction) * totalIssuance.toBigDecimal()
        val supportNeededIntegral = supportNeeded.toBigInteger()

        val currentSupport = tally.support.toBigDecimal()
        val totalSupport = totalIssuance.toBigDecimal()
        val supportFraction = currentSupport.divideOrNull(totalSupport, MathContext.DECIMAL64) ?: Perbill.ZERO

        return Threshold(
            value = supportNeededIntegral,
            currentlyPassing = tally.support >= supportNeededIntegral,
            getProjectedPassing(supportCurve, supportFraction)
        )
    }

    override fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill> {
        val approvalThreshold = approvalCurve.threshold(passedSinceDecidingFraction)
        val ayeFraction = tally.ayeVotes().fraction

        return Threshold(
            value = approvalThreshold,
            currentlyPassing = ayeFraction >= approvalThreshold,
            getProjectedPassing(approvalCurve, ayeFraction)
        )
    }

    private fun getProjectedPassing(curve: VotingCurve, fraction: Perbill): VotingThreshold.ProjectedPassing {
        val delay = curve.delay(fraction)
        val threshold = curve.threshold(delay)

        return VotingThreshold.ProjectedPassing(
            delayFraction = delay,
            passingInFuture = fraction >= threshold
        )
    }
}

fun Gov2VotingThreshold(trackInfo: TrackInfo): Gov2VotingThreshold {
    return Gov2VotingThreshold(
        supportCurve = trackInfo.minSupport!!,
        approvalCurve = trackInfo.minApproval!!
    )
}
