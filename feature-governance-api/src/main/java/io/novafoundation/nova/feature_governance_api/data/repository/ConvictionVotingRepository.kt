package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ConvictionVotingRepository {

    suspend fun voteLockingPeriod(chainId: ChainId): BlockNumber

    suspend fun maxTrackVotes(chainId: ChainId): BigInteger

    suspend fun trackLocksFlow(accountId: AccountId, chainId: ChainId): Flow<Map<TrackId, Balance>>

    suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting>

    suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackId: TrackId): Voting?

    suspend fun votersOf(referendumId: ReferendumId, chainId: ChainId): List<ReferendumVoter>
}
