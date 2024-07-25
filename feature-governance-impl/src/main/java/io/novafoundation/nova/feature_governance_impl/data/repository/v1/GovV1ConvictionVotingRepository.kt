package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.democracy
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votedFor
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.democracyRemoveVote
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.democracyUnlock
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.democracyVote
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindVoting
import io.novafoundation.nova.feature_governance_impl.data.repository.common.votersFor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class GovV1ConvictionVotingRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val balanceLocksRepository: BalanceLocksRepository,
) : ConvictionVotingRepository {

    override val voteLockId: String = DEMOCRACY_ID

    override suspend fun voteLockingPeriod(chainId: ChainId): BlockNumber {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.democracy().numberConstant("VoteLockingPeriod", runtime)
    }

    override suspend fun maxTrackVotes(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.democracy().numberConstant("MaxVotes", runtime)
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

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackIds: Collection<TrackId>): Map<TrackId, Voting> {
        unsupported()
    }

    override suspend fun votersOf(referendumId: ReferendumId, chain: Chain, type: VoteType): List<ReferendumVoter> {
        val allVotings = remoteStorageSource.query(chain.id) {
            runtime.metadata.democracy().storage("VotingOf").entries(
                keyExtractor = { it },
                binding = { decoded, _ -> bindVoting(decoded!!) }
            )
        }

        return allVotings.votersFor(referendumId)
            .filter { it.vote.votedFor(type) }
    }

    override fun ExtrinsicBuilder.unlock(accountId: AccountId, claimable: ClaimSchedule.UnlockChunk.Claimable) {
        claimable.actions.forEach { claimAction ->
            when (claimAction) {
                is ClaimSchedule.ClaimAction.RemoveVote -> {
                    removeVote(claimAction.trackId, claimAction.referendumId)
                }

                is ClaimSchedule.ClaimAction.Unlock -> {
                    democracyUnlock(accountId)
                }
            }
        }
    }

    override fun ExtrinsicBuilder.vote(referendumId: ReferendumId, vote: AccountVote) {
        democracyVote(referendumId, vote)
    }

    override fun ExtrinsicBuilder.removeVote(trackId: TrackId, referendumId: ReferendumId) {
        democracyRemoveVote(referendumId)
    }

    override fun isAbstainVotingAvailable(): Boolean {
        return false
    }

    private fun <T> T?.associatedWithTrack(): Map<TrackId, T> {
        return if (this != null) {
            mapOf(DemocracyTrackId to this)
        } else {
            emptyMap()
        }
    }

    private fun unsupported(): Nothing {
        error("Unsupported operation for Governance 1 voting")
    }
}
