package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill

data class TrackInfo(
    val id: TrackId,
    val name: String,
    val preparePeriod: BlockNumber,
    val decisionPeriod: BlockNumber,
    val minApproval: VotingCurve?,
    val minSupport: VotingCurve?
)

interface VotingCurve {

    val name: String

    fun threshold(x: Perbill): Perbill

    fun delay(y: Perbill): Perbill
}
