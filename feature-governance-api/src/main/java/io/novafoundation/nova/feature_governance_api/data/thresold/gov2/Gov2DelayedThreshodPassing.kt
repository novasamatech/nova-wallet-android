package io.novafoundation.nova.feature_governance_api.data.thresold.gov2

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.divideOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DelayedThresholdPassing
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.DelayedThresholdPassing.*
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeVotes
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class Gov2DelayedThresholdPassing(
    val supportCurve: VotingCurve,
    val approvalCurve: VotingCurve
) : DelayedThresholdPassing {

    override fun supportPassingInFuture(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): DelayedPassing {
        val supportFraction = tally.support.toBigDecimal().divideOrNull(totalIssuance.toBigDecimal()) ?: Perbill.ZERO

        return getDelay(supportCurve, supportFraction)
    }

    override fun ayePassingInFuture(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): DelayedPassing {
        return getDelay(approvalCurve, tally.ayeVotes().fraction)
    }

    private fun getDelay(curve: VotingCurve, fraction: Perbill): DelayedPassing {
        val delay = curve.delay(fraction)
        val threshold = curve.threshold(delay)

        return DelayedPassing(
            delayFraction = delay,
            passingInFuture = fraction >= threshold
        )
    }
}

fun Gov2DelayedThresholdPassing(trackInfo: TrackInfo): Gov2DelayedThresholdPassing {
    return Gov2DelayedThresholdPassing(
        supportCurve = trackInfo.minSupport!!,
        approvalCurve = trackInfo.minApproval!!
    )
}
