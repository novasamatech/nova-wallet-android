package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface ConvictionVotingRepository {

    suspend fun trackLocksFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Balance>

    suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting>

    suspend fun votersOf(referendumId: ReferendumId, chainId: ChainId): List<ReferendumVoter>
}
