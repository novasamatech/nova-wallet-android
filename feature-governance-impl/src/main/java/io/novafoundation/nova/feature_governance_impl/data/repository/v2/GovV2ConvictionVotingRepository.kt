package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.utils.convictionVoting
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingRemoveVote
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingUnlock
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingVote
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindVoting
import io.novafoundation.nova.feature_governance_impl.data.repository.common.votersFor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
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

class GovV2ConvictionVotingRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : ConvictionVotingRepository {

    override val voteLockId: String = "pyconvot"

    override suspend fun voteLockingPeriod(chainId: ChainId): BlockNumber {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.convictionVoting().numberConstant("VoteLockingPeriod", runtime)
    }

    override suspend fun maxTrackVotes(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.convictionVoting().numberConstant("MaxVotes", runtime)
    }

    override fun trackLocksFlow(accountId: AccountId, chainAssetId: FullChainAssetId): Flow<Map<TrackId, Balance>> {
        return remoteStorageSource.subscribe(chainAssetId.chainId) {
            runtime.metadata.convictionVoting().storage("ClassLocksFor").observe(accountId, binding = ::bindTrackLocks)
                .map { it.toMap() }
        }
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting> {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").entries(
                accountId,
                keyExtractor = { (_: AccountId, trackId: BigInteger) -> TrackId(trackId) },
                binding = { decoded, _ -> bindVoting(decoded!!) }
            )
        }
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackId: TrackId): Voting? {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").query(
                accountId,
                binding = { decoded -> decoded?.let(::bindVoting) }
            )
        }
    }

    override suspend fun votersOf(referendumId: ReferendumId, chainId: ChainId): List<ReferendumVoter> {
        val allVotings = remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").entries(
                keyExtractor = { it },
                binding = { decoded, _ -> bindVoting(decoded!!) }
            )
        }

        return allVotings.votersFor(referendumId)
    }

    override fun ExtrinsicBuilder.unlock(accountId: AccountId, claimable: ClaimSchedule.UnlockChunk.Claimable) {
        claimable.actions.forEach { claimAction ->
            when (claimAction) {
                is ClaimSchedule.ClaimAction.RemoveVote -> {
                    convictionVotingRemoveVote(claimAction.trackId, claimAction.referendumId)
                }

                is ClaimSchedule.ClaimAction.Unlock -> {
                    convictionVotingUnlock(claimAction.trackId, accountId)
                }
            }
        }
    }

    override fun ExtrinsicBuilder.vote(referendumId: ReferendumId, vote: AccountVote) {
        convictionVotingVote(referendumId, vote)
    }

    private fun bindTrackLocks(decoded: Any?): List<Pair<TrackId, Balance>> {
        return bindList(decoded) { item ->
            val (trackId, balance) = item.castToList()

            TrackId(bindNumber(trackId)) to bindNumber(balance)
        }
    }
}
