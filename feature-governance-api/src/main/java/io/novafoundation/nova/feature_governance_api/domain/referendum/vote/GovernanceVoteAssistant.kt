package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface GovernanceVoteAssistant {

    val onChainReferenda: List<OnChainReferendum>

    val trackVoting: List<Voting>

    suspend fun estimateLocksAfterVoting(votes: Map<ReferendumId, AccountVote>, asset: Asset): LocksChange

    suspend fun reusableLocks(): List<ReusableLock>
}

suspend fun GovernanceVoteAssistant.estimateLocksAfterVoting(referendumId: ReferendumId, accountVote: AccountVote, asset: Asset): LocksChange {
    return estimateLocksAfterVoting(mapOf(referendumId to accountVote), asset)
}
