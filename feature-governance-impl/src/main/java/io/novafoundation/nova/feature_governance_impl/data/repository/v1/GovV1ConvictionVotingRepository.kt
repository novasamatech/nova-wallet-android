package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigInteger

class GovV1ConvictionVotingRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : ConvictionVotingRepository {

    override val voteLockId: String = "democrac"

    override suspend fun voteLockingPeriod(chainId: ChainId): BlockNumber {
        // TODO
        return Balance.ZERO
    }

    override suspend fun maxTrackVotes(chainId: ChainId): BigInteger {
        // TODO
        return Balance.ZERO
    }

    override fun trackLocksFlow(accountId: AccountId, chainId: ChainId): Flow<Map<TrackId, Balance>> {
        // TODO
        return flowOf(emptyMap())
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting> {
        // TODO
        return emptyMap()
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackId: TrackId): Voting? {
        // TODO
        return null
    }

    override suspend fun votersOf(referendumId: ReferendumId, chainId: ChainId): List<ReferendumVoter> {
        // TODO
        return emptyList()
    }

    override fun ExtrinsicBuilder.unlock(accountId: AccountId, claimable: ClaimSchedule.UnlockChunk.Claimable) {
        // TODO
    }
}
