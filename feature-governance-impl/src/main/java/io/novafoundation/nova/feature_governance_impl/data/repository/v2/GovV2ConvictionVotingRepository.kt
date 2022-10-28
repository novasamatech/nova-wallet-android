package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.convictionVoting
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PriorLock
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingRemoveVote
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingUnlock
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
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

    override fun trackLocksFlow(accountId: AccountId, chainId: ChainId): Flow<Map<TrackId, Balance>> {
        return remoteStorageSource.subscribe(chainId) {
            runtime.metadata.convictionVoting().storage("ClassLocksFor").observe(accountId, binding = ::bindTrackLocks)
                .map { it.toMap() }
        }
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId): Map<TrackId, Voting> {
        return remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").entries(
                accountId,
                keyExtractor = { (_: AccountId, trackId: BigInteger) -> TrackId(trackId) },
                binding = { decoded, _ -> bindVoting(decoded) }
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
                binding = { decoded, _ -> bindVoting(decoded) }
            )
        }

        return allVotings.mapNotNull { (keyComponents, voting) ->
            val (voterId: AccountId, _: BigInteger) = keyComponents
            val votes = voting.votes()

            votes[referendumId]?.let { accountVote ->
                ReferendumVoter(
                    accountId = voterId,
                    vote = accountVote
                )
            }
        }
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

    private fun bindTrackLocks(decoded: Any?): List<Pair<TrackId, Balance>> {
        return bindList(decoded) { item ->
            val (trackId, balance) = item.castToList()

            TrackId(bindNumber(trackId)) to bindNumber(balance)
        }
    }

    private fun bindVoting(decoded: Any?): Voting {
        decoded.castToDictEnum()

        return when (decoded.name) {
            "Casting" -> {
                val casting = decoded.value.castToStruct()

                val votes = bindVotes(casting["votes"])
                val prior = bindPriorLock(casting["prior"])

                Voting.Casting(votes, prior)
            }

            "Delegating" -> {
                val delegating = decoded.value.castToStruct()

                val balance = bindNumber(delegating["balance"])
                val prior = bindPriorLock(delegating["prior"])

                Voting.Delegating(balance, prior)
            }

            else -> incompatible()
        }
    }

    private fun bindVotes(decoded: Any?): Map<ReferendumId, AccountVote> {
        return bindList(decoded) { item ->
            val (trackId, accountVote) = item.castToList()

            ReferendumId(bindNumber(trackId)) to bindAccountVote(accountVote)
        }.toMap()
    }

    private fun bindAccountVote(decoded: Any?): AccountVote {
        decoded.castToDictEnum()

        return when (decoded.name) {
            "Standard" -> {
                val standardVote = decoded.value.castToStruct()

                AccountVote.Standard(
                    vote = bindVote(standardVote["vote"]),
                    balance = bindNumber(standardVote["balance"])
                )
            }

            "Split" -> AccountVote.Split

            else -> incompatible()
        }
    }

    private fun bindPriorLock(decoded: Any?): PriorLock {
        // 2-tuple
        val (unlockAt, amount) = decoded.castToList()

        return PriorLock(
            unlockAt = bindBlockNumber(unlockAt),
            amount = bindNumber(amount)
        )
    }

    private fun bindVote(decoded: Any?): Vote {
        return decoded.cast()
    }
}
