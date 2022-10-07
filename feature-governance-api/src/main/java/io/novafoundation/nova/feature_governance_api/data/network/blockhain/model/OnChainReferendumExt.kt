package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.extensions.toHexString

fun OnChainReferendum.proposalHash(): String? {
    return status.asOngoing()?.proposalHash?.toHexString(withPrefix = true)
}

fun OnChainReferendum.track(): TrackId? {
    return status.asOngoing()?.track
}

private fun OnChainReferendumStatus.asOngoing(): OnChainReferendumStatus.Ongoing? {
    return castOrNull<OnChainReferendumStatus.Ongoing>()
}

fun Tally.ayeFraction(): Perbill {
    val totalVotes = ayes + nays

    if (totalVotes == Balance.ZERO) return Perbill.ZERO

    return ayes.divideToDecimal(totalVotes)
}

fun Tally.nayFraction(): Perbill {
    val totalVotes = ayes + nays

    if (totalVotes == Balance.ZERO) return Perbill.ZERO

    return nays.divideToDecimal(totalVotes)
}

fun TrackInfo.supportThreshold(x: Perbill, totalIssuance: Balance): Balance {
    val fractionThreshold = minSupport.threshold(x)
    val balanceThreshold = fractionThreshold * totalIssuance.toBigDecimal()

    return balanceThreshold.toBigInteger()
}
