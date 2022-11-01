package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.convictionVoting
import io.novafoundation.nova.common.utils.democracy
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindVoting
import io.novafoundation.nova.feature_governance_impl.data.repository.common.votersFor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class GovV1ConvictionVotingRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val balanceLocksRepository: BalanceLocksRepository,
) : ConvictionVotingRepository {

    override val voteLockId: String = "democrac"

    override suspend fun voteLockingPeriod(chainId: ChainId): BlockNumber {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.democracy().numberConstant("VoteLockingPeriod", runtime)
    }

    override suspend fun maxTrackVotes(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.convictionVoting().numberConstant("MaxVotes", runtime)
    }

    override fun trackLocksFlow(accountId: AccountId, chainAssetId: FullChainAssetId): Flow<Map<TrackId, Balance>> {
        return flowOfAll {
            val chainAsset = chainRegistry.asset(chainAssetId)

            balanceLocksRepository.observeBalanceLock(chainAsset, voteLockId)
                .map { lock -> lock?.amountInPlanks.associatedWithTrack() }
        }
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting> {
        return votingFor(accountId, chainId, DemocracyTrackId).associatedWithTrack()
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackId: TrackId): Voting? {
        if (trackId != DemocracyTrackId) return null

        return remoteStorageSource.query(chainId) {
            runtime.metadata.democracy().storage("VotingOf").query(
                accountId,
                binding = { decoded -> decoded?.let(::bindVoting) }
            )
        }
    }

    override suspend fun votersOf(referendumId: ReferendumId, chainId: ChainId): List<ReferendumVoter> {
        val allVotings = remoteStorageSource.query(chainId) {
            runtime.metadata.democracy().storage("VotingOf").entries(
                keyExtractor = { it },
                binding = { decoded, _ -> bindVoting(decoded!!) }
            )
        }

        return allVotings.votersFor(referendumId)
    }

    override fun ExtrinsicBuilder.unlock(accountId: AccountId, claimable: ClaimSchedule.UnlockChunk.Claimable) {
        // TODO
    }

    private fun <T> T?.associatedWithTrack(): Map<TrackId, T> {
        return if (this != null) {
            mapOf(DemocracyTrackId to this)
        } else {
            emptyMap()
        }
    }
}
