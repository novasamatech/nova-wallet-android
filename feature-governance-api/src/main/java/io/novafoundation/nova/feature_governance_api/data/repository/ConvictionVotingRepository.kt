package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.delegations
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLockId
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ConvictionVotingRepository {

    val voteLockId: BalanceLockId

    suspend fun maxAvailableForVote(asset: Asset): Balance

    suspend fun voteLockingPeriod(chainId: ChainId): BlockNumber

    suspend fun maxTrackVotes(chainId: ChainId): BigInteger

    fun trackLocksFlow(accountId: AccountId, chainAssetId: FullChainAssetId): Flow<Map<TrackId, Balance>>

    suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting>

    suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackId: TrackId): Voting?

    suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackIds: Collection<TrackId>): Map<TrackId, Voting>

    suspend fun votersOf(referendumId: ReferendumId, chain: Chain, type: VoteType): List<ReferendumVoter>

    suspend fun delegatingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting.Delegating> {
        return votingFor(accountId, chainId).delegations()
    }

    fun ExtrinsicBuilder.unlock(accountId: AccountId, claimable: ClaimSchedule.UnlockChunk.Claimable)

    fun ExtrinsicBuilder.vote(referendumId: ReferendumId, vote: AccountVote)

    fun CallBuilder.vote(referendumId: ReferendumId, vote: AccountVote)
    fun ExtrinsicBuilder.removeVote(trackId: TrackId, referendumId: ReferendumId)

    fun isAbstainVotingAvailable(): Boolean

    suspend fun abstainVotingDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumVotingDetails?

    suspend fun fullVotingDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumVotingDetails?
}
