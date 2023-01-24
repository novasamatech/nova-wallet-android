package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote.UserVote
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.metadata.DelegateMetadataApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.metadata.getDelegatesMetadata
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.AllHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDelegatorsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDetailedStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DirectHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateDelegatorsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegatedVoteRemote
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DirectVoteRemote
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceDelegations
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class Gov2DelegationsRepository(
    private val delegationsSubqueryApi: DelegationsSubqueryApi,
    private val delegateMetadataApi: DelegateMetadataApi,
) : DelegationsRepository {

    override suspend fun isDelegationSupported(): Boolean {
        return true
    }

    override suspend fun getDelegatesStats(
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain
    ): List<DelegateStats> {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return emptyList()
        val request = DelegateStatsRequest(recentVotesBlockThreshold)

        val response = delegationsSubqueryApi.getDelegateStats(externalApiLink, request)
        val delegateStats = response.data.delegates.nodes

        return delegateStats.map { delegate ->
            DelegateStats(
                accountId = chain.accountIdOf(delegate.address),
                delegationsCount = delegate.delegators,
                delegatedVotes = delegate.delegatorVotes,
                recentVotes = delegate.delegateVotes.totalCount
            )
        }
    }

    override suspend fun getDetailedDelegateStats(
        delegateAddress: String,
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain
    ): DelegateDetailedStats? {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return null
        val request = DelegateDetailedStatsRequest(delegateAddress, recentVotesBlockThreshold)

        val response = delegationsSubqueryApi.getDetailedDelegateStats(externalApiLink, request)
        val delegateStats = response.data.delegates.nodes.firstOrNull() ?: return null

        return DelegateDetailedStats(
            accountId = chain.accountIdOf(delegateAddress),
            delegationsCount = delegateStats.delegators,
            delegatedVotes = delegateStats.delegatorVotes,
            recentVotes = delegateStats.recentVotes.totalCount,
            allVotes = delegateStats.allVotes.totalCount
        )
    }

    override suspend fun getDelegatesMetadata(chain: Chain): List<DelegateMetadata> {
        return delegateMetadataApi.getDelegatesMetadata(chain).mapNotNull {
            val accountId = chain.accountIdOrNull(it.address) ?: return@mapNotNull null

            DelegateMetadata(
                accountId = accountId,
                shortDescription = it.shortDescription,
                longDescription = it.longDescription,
                profileImageUrl = it.image,
                isOrganization = it.isOrganization,
                name = it.name
            )
        }
    }

    override suspend fun getDelegateMetadata(chain: Chain, delegate: AccountId): DelegateMetadata? {
        return getDelegatesMetadata(chain)
            .find { it.accountId.contentEquals(delegate) }
    }

    override suspend fun getDelegationsTo(delegate: AccountId, chain: Chain): List<Delegation> {
        return accountSubQueryRequest(delegate, chain) { externalApiLink, delegateAddress ->
            val request = DelegateDelegatorsRequest(delegateAddress)
            val response = delegationsSubqueryApi.getDelegateDelegators(externalApiLink, request)
            response.data.delegations.nodes.map { mapDelegationFromRemote(it, chain, delegate) }
        }.orEmpty()
    }

    override suspend fun allHistoricalVotesOf(user: AccountId, chain: Chain): Map<ReferendumId, UserVote>? {
        return accountSubQueryRequest(user, chain) { externalApiLink, userAddress ->
            val request = AllHistoricalVotesRequest(userAddress)
            val response = delegationsSubqueryApi.getAllHistoricalVotes(externalApiLink, request)

            val direct = response.data.direct.toUserVoteMap()
            val delegated = response.data.delegated.toUserVoteMap(chain)

            (direct + delegated).filterNotNull()
        }
    }

    override suspend fun directHistoricalVotesOf(user: AccountId, chain: Chain): Map<ReferendumId, UserVote.Direct>? {
        return accountSubQueryRequest(user, chain) { externalApiLink, userAddress ->
            val request = DirectHistoricalVotesRequest(userAddress)
            val response = delegationsSubqueryApi.getDirectHistoricalVotes(externalApiLink, request)

            response.data.direct.toUserVoteMap().filterNotNull()
        }
    }

    private fun SubQueryNodes<DirectVoteRemote>.toUserVoteMap(): Map<ReferendumId, UserVote.Direct?> {
        return nodes.associateBy(
            keySelector = { ReferendumId(it.referendumId) },
            valueTransform = { directVoteRemote ->
                val standardVote = directVoteRemote.standardVote ?: return@associateBy null

                UserVote.Direct(
                    AccountVote.Standard(
                        balance = standardVote.vote.amount,
                        vote = Vote(
                            aye = directVoteRemote.standardVote.aye,
                            conviction = mapConvictionFromRemote(directVoteRemote.standardVote.vote.conviction)
                        )
                    ),
                )
            }
        )
    }

    private fun SubQueryNodes<DelegatedVoteRemote>.toUserVoteMap(chain: Chain): Map<ReferendumId, UserVote.Delegated?> {
        return nodes.associateBy(
            keySelector = { ReferendumId(it.parent.referendumId) },
            valueTransform = { delegatedVoteRemote ->
                val aye = delegatedVoteRemote.parent.standardVote?.aye ?: return@associateBy null
                val standardVote = delegatedVoteRemote.vote

                UserVote.Delegated(
                    delegate = chain.accountIdOf(delegatedVoteRemote.parent.delegate.address),
                    vote = AccountVote.Standard(
                        balance = standardVote.amount,
                        vote = Vote(
                            aye = aye,
                            conviction = mapConvictionFromRemote(delegatedVoteRemote.vote.conviction)
                        )
                    ),
                )
            }
        )
    }

    private fun mapDelegationFromRemote(
        delegation: DelegateDelegatorsResponse.DelegatorRemote,
        chain: Chain,
        delegate: AccountId
    ): Delegation {
        return Delegation(
            vote = Delegation.Vote(
                amount = delegation.delegation.amount,
                conviction = mapConvictionFromRemote(delegation.delegation.conviction)
            ),
            delegator = chain.accountIdOf(delegation.address),
            delegate = delegate
        )
    }

    private fun mapConvictionFromRemote(remote: String): Conviction {
        return Conviction.values().first { it.name == remote }
    }

    private inline fun <R> accountSubQueryRequest(
        accountId: AccountId,
        chain: Chain,
        action: (url: String, address: String) -> R
    ): R? {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return null
        val address = chain.addressOf(accountId)

        return runCatching { action(externalApiLink, address) }
            .onFailure { Log.e(LOG_TAG, "Failed to execute subquery request", it) }
            .getOrNull()
    }
}
