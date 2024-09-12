package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface VotingLocksEstimator {

    val onChainReferenda: List<OnChainReferendum>

    suspend fun estimateLocks(votes: Map<ReferendumId, AccountVote>, asset: Asset): LocksChange

    suspend fun reusableLocks(): List<ReusableLock>
}
