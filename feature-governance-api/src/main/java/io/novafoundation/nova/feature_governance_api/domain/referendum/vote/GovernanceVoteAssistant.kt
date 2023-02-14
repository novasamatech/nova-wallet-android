package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

interface GovernanceVoteAssistant {

    val onChainReferendum: OnChainReferendum

    val trackVoting: Voting?

    suspend fun estimateLocksAfterVoting(amount: Balance, conviction: Conviction, asset: Asset): LocksChange

    suspend fun reusableLocks(): List<ReusableLock>
}
