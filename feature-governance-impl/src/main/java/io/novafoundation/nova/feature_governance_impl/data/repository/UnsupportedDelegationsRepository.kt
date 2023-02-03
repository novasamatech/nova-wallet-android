package io.novafoundation.nova.feature_governance_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote.UserVote
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

class UnsupportedDelegationsRepository : DelegationsRepository {

    override suspend fun isDelegationSupported(): Boolean {
        return false
    }

    override suspend fun getDelegatesStats(recentVotesBlockThreshold: BlockNumber, chain: Chain): List<DelegateStats> {
        return emptyList()
    }

    override suspend fun getDetailedDelegateStats(delegateAddress: String, recentVotesBlockThreshold: BlockNumber, chain: Chain): DelegateDetailedStats? {
        return null
    }

    override suspend fun getDelegatesMetadata(chain: Chain): List<DelegateMetadata> {
        return emptyList()
    }

    override suspend fun getDelegateMetadata(chain: Chain, delegate: AccountId): DelegateMetadata? {
        return null
    }

    override suspend fun getDelegationsTo(delegate: AccountId, chain: Chain): List<Delegation> {
        return emptyList()
    }

    override suspend fun allHistoricalVotesOf(user: AccountId, chain: Chain): Map<ReferendumId, UserVote>? {
        return null
    }

    override suspend fun historicalVoteOf(user: AccountId, referendumId: ReferendumId, chain: Chain): UserVote? {
        return null
    }

    override suspend fun directHistoricalVotesOf(
        user: AccountId,
        chain: Chain,
        recentVotesBlockThreshold: BlockNumber?
    ): Map<ReferendumId, UserVote.Direct>? {
        return null
    }

    override suspend fun ExtrinsicBuilder.delegate(delegate: AccountId, trackId: TrackId, amount: Balance, conviction: Conviction) {
        error("Unsupported")
    }
}
