package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote.UserVote
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingDelegate
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingUndelegate
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.metadata.DelegateMetadataApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.metadata.getDelegatesMetadata
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.AllHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateDelegatorsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateDetailedStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateStatsByAddressesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DirectHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DelegateDelegatorsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DelegateStatsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DelegatedVoteRemote
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DirectVoteRemote
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.mapMultiVoteRemoteToAccountVote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceDelegations
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.mapConvictionFromString
import io.novasama.substrate_sdk_android.runtime.AccountId

class Gov2DelegationsRepository(
    private val delegationsSubqueryApi: DelegationsSubqueryApi,
    private val delegateMetadataApi: DelegateMetadataApi,
) : DelegationsRepository {

    override suspend fun isDelegationSupported(chain: Chain): Boolean {
        // we heavy rely on SubQuery API for delegations so we require it to be present
        return chain.externalApi<GovernanceDelegations>() != null
    }

    override suspend fun getDelegatesStats(
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain
    ): List<DelegateStats> {
        return runCatching {
            val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return emptyList()
            val request = DelegateStatsRequest(recentVotesBlockThreshold)
            val response = delegationsSubqueryApi.getDelegateStats(externalApiLink, request)
            val delegateStats = response.data.delegates.nodes

            mapDelegateStats(delegateStats, chain)
        }.getOrNull()
            .orEmpty()
    }

    override suspend fun getDelegatesStatsByAccountIds(recentVotesBlockThreshold: BlockNumber, accountIds: List<AccountId>, chain: Chain): List<DelegateStats> {
        return runCatching {
            val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return emptyList()
            val addresses = accountIds.map { chain.addressOf(it) }
            val request = DelegateStatsByAddressesRequest(recentVotesBlockThreshold, addresses = addresses)
            val response = delegationsSubqueryApi.getDelegateStats(externalApiLink, request)
            val delegateStats = response.data.delegates.nodes

            mapDelegateStats(delegateStats, chain)
        }.getOrNull()
            .orEmpty()
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

    override suspend fun historicalVoteOf(user: AccountId, referendumId: ReferendumId, chain: Chain): UserVote? {
        return allHistoricalVotesOf(user, chain)?.get(referendumId)
    }

    override suspend fun directHistoricalVotesOf(
        user: AccountId,
        chain: Chain,
        recentVotesBlockThreshold: BlockNumber?
    ): Map<ReferendumId, UserVote.Direct>? {
        val blockThreshold = recentVotesBlockThreshold ?: BlockNumber.ZERO

        return accountSubQueryRequest(user, chain) { externalApiLink, userAddress ->
            val request = DirectHistoricalVotesRequest(userAddress, blockThreshold)
            val response = delegationsSubqueryApi.getDirectHistoricalVotes(externalApiLink, request)

            response.data.direct.toUserVoteMap().filterNotNull()
        }
    }

    override suspend fun CallBuilder.delegate(delegate: AccountId, trackId: TrackId, amount: Balance, conviction: Conviction) {
        convictionVotingDelegate(delegate, trackId, amount, conviction)
    }

    override suspend fun CallBuilder.undelegate(trackId: TrackId) {
        convictionVotingUndelegate(trackId)
    }

    private fun SubQueryNodes<DirectVoteRemote>.toUserVoteMap(): Map<ReferendumId, UserVote.Direct?> {
        return nodes.associateBy(
            keySelector = { ReferendumId(it.referendumId) },
            valueTransform = { directVoteRemote -> UserVote.Direct(mapMultiVoteRemoteToAccountVote(directVoteRemote)) }
        )
    }

    private fun SubQueryNodes<DelegatedVoteRemote>.toUserVoteMap(chain: Chain): Map<ReferendumId, UserVote.Delegated?> {
        return nodes.associateBy(
            keySelector = { ReferendumId(it.parent.referendumId) },
            valueTransform = { delegatedVoteRemote ->
                // delegated votes do not participate in any vote rather than standard
                val aye = delegatedVoteRemote.parent.standardVote?.aye ?: return@associateBy null
                val standardVote = delegatedVoteRemote.vote

                UserVote.Delegated(
                    delegate = chain.accountIdOf(delegatedVoteRemote.parent.delegateId),
                    vote = AccountVote.Standard(
                        balance = standardVote.amount,
                        vote = Vote(
                            aye = aye,
                            conviction = mapConvictionFromString(delegatedVoteRemote.vote.conviction)
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
                conviction = mapConvictionFromString(delegation.delegation.conviction)
            ),
            delegator = chain.accountIdOf(delegation.address),
            delegate = delegate
        )
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

    private fun mapDelegateStats(delegateStats: List<DelegateStatsResponse.Delegate>, chain: Chain): List<DelegateStats> {
        return delegateStats.map { delegate ->
            DelegateStats(
                accountId = chain.accountIdOf(delegate.address),
                delegationsCount = delegate.delegators,
                delegatedVotes = delegate.delegatorVotes,
                recentVotes = delegate.delegateVotes.totalCount
            )
        }
    }
}
