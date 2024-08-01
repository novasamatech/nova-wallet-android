package io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails.VotingInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigDecimal

class OffChainReferendumVotingDetails(
    val trackId: TrackId,
    val votingInfo: VotingInfo
) {

    sealed interface VotingInfo {

        class Abstain(val abstain: Balance) : VotingInfo

        class Full(
            val aye: BigDecimal,
            val nay: BigDecimal,
            val abstain: Balance,
            val support: Balance
        ) : VotingInfo {
            companion object
        }
    }
}

fun VotingInfo.Full.Companion.empty() = VotingInfo.Full(
    aye = BigDecimal.ZERO,
    nay = BigDecimal.ZERO,
    abstain = Balance.ZERO,
    support = Balance.ZERO
)

operator fun VotingInfo.Full.plus(other: VotingInfo.Full): VotingInfo.Full {
    return VotingInfo.Full(
        aye = this.aye + other.aye,
        nay = this.nay + other.nay,
        abstain = this.abstain + other.abstain,
        support = this.support + other.support
    )
}

fun VotingInfo.Full.toTally(): Tally = Tally(
    ayes = this.aye.toBigInteger(),
    nays = this.nay.toBigInteger(),
    support = this.support
)

fun VotingInfo.getAbstain(): Balance {
    return when (this) {
        is VotingInfo.Abstain -> this.abstain
        is VotingInfo.Full -> this.abstain
    }
}

fun VotingInfo.toTallyOrNull(): Tally? {
    return when (this) {
        is VotingInfo.Full -> toTally()
        is VotingInfo.Abstain -> null
    }
}
