package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.utils.convictionVoting
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.sum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.isAye
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votedFor
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails.VotingInfo
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.empty
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.plus
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingRemoveVote
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingUnlock
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingVote
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.ReferendumSplitAbstainVotersRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.ReferendumVotersRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.ReferendumVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.ReferendumVoterRemote
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.mapMultiVoteRemoteToAccountVote
import io.novafoundation.nova.feature_governance_impl.data.repository.common.bindVoting
import io.novafoundation.nova.feature_governance_impl.data.repository.common.toOffChainVotes
import io.novafoundation.nova.feature_governance_impl.data.repository.common.votersFor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.mapConvictionFromString
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class GovV2ConvictionVotingRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val delegateSubqueryApi: DelegationsSubqueryApi
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

    override suspend fun observeVotingFor(accountId: AccountId, chainId: ChainId): Flow<Map<TrackId, Voting>> {
        return flowOf { emptyMap() }
        return remoteStorageSource.subscribe(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").observeByPrefix(
                accountId,
                keyExtractor = { (_: AccountId, trackId: BigInteger) -> TrackId(trackId) },
                binding = { decoded, _ -> bindVoting(decoded!!) }
            )
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
                trackId.value,
                binding = { decoded -> decoded?.let(::bindVoting) }
            )
        }
    }

    override suspend fun votingFor(accountId: AccountId, chainId: ChainId, trackIds: Collection<TrackId>): Map<TrackId, Voting> {
        val keys = trackIds.map { listOf(accountId, it.value) }

        return remoteStorageSource.query(chainId) {
            runtime.metadata.convictionVoting().storage("VotingFor").entries(
                keysArguments = keys,
                keyExtractor = { (_: AccountId, trackId: BigInteger) -> TrackId(trackId) },
                binding = { decoded, _ -> decoded?.let(::bindVoting) }
            )
        }.filterNotNull()
    }

    override suspend fun votersOf(referendumId: ReferendumId, chain: Chain, type: VoteType): List<ReferendumVoter> {
        val governanceDelegationsExternalApi = chain.externalApi<Chain.ExternalApi.GovernanceDelegations>()
        return if (governanceDelegationsExternalApi != null) {
            runCatching { getVotersFromIndexer(referendumId, chain, governanceDelegationsExternalApi, type) }
                .getOrElse { getVotersFromChain(referendumId, chain, type) }
        } else {
            getVotersFromChain(referendumId, chain, type)
        }
    }

    override suspend fun abstainVotingDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumVotingDetails? {
        val api = chain.externalApi<Chain.ExternalApi.GovernanceDelegations>() ?: return null

        return runCatching {
            val request = ReferendumSplitAbstainVotersRequest(referendumId.value)
            val response = delegateSubqueryApi.getReferendumAbstainVoters(api.url, request)
            val trackId = TrackId(response.data.referendum.trackId)
            val abstainAmountSum = response.data
                .referendum
                .castingVotings
                .nodes
                .mapNotNull { it.splitAbstainVote?.abstainAmount }
                .sum()

            val abstainVotes = abstainAmountSum.toBigDecimal() * Conviction.None.amountMultiplier()
            OffChainReferendumVotingDetails(trackId, VotingInfo.Abstain(abstainVotes))
        }.getOrNull()
    }

    private suspend fun getVotersFromIndexer(
        referendumId: ReferendumId,
        chain: Chain,
        api: Chain.ExternalApi.GovernanceDelegations,
        type: VoteType
    ): List<ReferendumVoter> {
        val request = ReferendumVotersRequest(referendumId.value, type.isAye())
        val response = delegateSubqueryApi.getReferendumVoters(api.url, request)
        return response.data
            .voters
            .nodes
            .mapNotNull { mapVoterFromRemote(it, chain, type) }
    }

    private suspend fun getVotersFromChain(referendumId: ReferendumId, chain: Chain, type: VoteType): List<ReferendumVoter> {
        val allVotings = remoteStorageSource.query(chain.id) {
            runtime.metadata.convictionVoting().storage("VotingFor").entries(
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
                    convictionVotingUnlock(claimAction.trackId, accountId)
                }
            }
        }
    }

    override fun ExtrinsicBuilder.vote(referendumId: ReferendumId, vote: AccountVote) {
        convictionVotingVote(referendumId, vote)
    }

    override fun ExtrinsicBuilder.removeVote(trackId: TrackId, referendumId: ReferendumId) {
        convictionVotingRemoveVote(trackId, referendumId)
    }

    override fun isAbstainVotingAvailable(): Boolean {
        return true
    }

    override suspend fun fullVotingDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumVotingDetails? {
        val api = chain.externalApi<Chain.ExternalApi.GovernanceDelegations>() ?: return null

        return runCatching {
            val referendum = delegateSubqueryApi.getReferendumVotes(api.url, ReferendumVotesRequest(referendumId.value))
                .data
                .referendum

            val voters = referendum.castingVotings.nodes

            var totalVoting = VotingInfo.Full.empty()

            voters.forEach {
                totalVoting += it.toOffChainVotes()
            }

            OffChainReferendumVotingDetails(TrackId(referendum.trackId), totalVoting)
        }.getOrNull()
    }

    private fun bindTrackLocks(decoded: Any?): List<Pair<TrackId, Balance>> {
        return bindList(decoded) { item ->
            val (trackId, balance) = item.castToList()

            TrackId(bindNumber(trackId)) to bindNumber(balance)
        }
    }

    private fun mapVoterFromRemote(voter: ReferendumVoterRemote, chain: Chain, expectedType: VoteType): ReferendumVoter? {
        val accountVote = mapMultiVoteRemoteToAccountVote(voter)
        if (!accountVote.votedFor(expectedType)) return null

        val delegators = voter.delegatorVotes.nodes

        return ReferendumVoter(
            accountId = chain.accountIdOf(voter.voterId),
            vote = accountVote,
            delegators = delegators.map {
                Delegation(
                    vote = Delegation.Vote(it.vote.amount, mapConvictionFromString(it.vote.conviction)),
                    delegator = chain.accountIdOf(it.delegatorId),
                    delegate = chain.accountIdOf(voter.voterId),
                )
            }
        )
    }
}
