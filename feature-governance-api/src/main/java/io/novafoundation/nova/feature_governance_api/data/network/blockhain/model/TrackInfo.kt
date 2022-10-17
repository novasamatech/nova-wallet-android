package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

data class TrackInfo(
    val id: TrackId,
    val name: String,
    val maxDeciding: BigInteger,
    val decisionDeposit: Balance,
    val preparePeriod: BlockNumber,
    val decisionPeriod: BlockNumber,
    val confirmPeriod: BlockNumber,
    val minEnactmentPeriod: BlockNumber,
    val minApproval: VotingCurve,
    val minSupport: VotingCurve
)

interface VotingCurve {

    val name: String

    fun threshold(x: Perbill): Perbill
}
