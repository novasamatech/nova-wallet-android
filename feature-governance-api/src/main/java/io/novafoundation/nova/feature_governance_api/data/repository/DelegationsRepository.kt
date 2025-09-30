package io.novafoundation.nova.feature_governance_api.data.repository

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote.UserVote
import io.novafoundation.nova.feature_governance_api.data.repository.common.RecentVotesDateThreshold
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novasama.substrate_sdk_android.runtime.AccountId

interface DelegationsRepository {

    suspend fun isDelegationSupported(chain: Chain): Boolean

    suspend fun getDelegatesStats(
        recentVotesDateThreshold: RecentVotesDateThreshold,
        chain: Chain
    ): List<DelegateStats>

    suspend fun getDelegatesStatsByAccountIds(
        recentVotesDateThreshold: RecentVotesDateThreshold,
        accountIds: List<AccountId>,
        chain: Chain
    ): List<DelegateStats>

    suspend fun getDetailedDelegateStats(
        delegateAddress: String,
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain,
    ): DelegateDetailedStats?

    suspend fun getDelegatesMetadata(chain: Chain): List<DelegateMetadata>

    suspend fun getDelegateMetadata(chain: Chain, delegate: AccountId): DelegateMetadata?

    suspend fun getDelegationsTo(delegate: AccountId, chain: Chain): List<Delegation>

    suspend fun allHistoricalVotesOf(user: AccountId, chain: Chain): Map<ReferendumId, UserVote>?

    suspend fun historicalVoteOf(user: AccountId, referendumId: ReferendumId, chain: Chain): UserVote?

    suspend fun directHistoricalVotesOf(
        user: AccountId,
        chain: Chain,
        recentVotesBlockThreshold: BlockNumber?
    ): Map<ReferendumId, UserVote.Direct>?

    suspend fun CallBuilder.delegate(
        delegate: AccountId,
        trackId: TrackId,
        amount: Balance,
        conviction: Conviction
    )

    suspend fun CallBuilder.undelegate(trackId: TrackId)
}

suspend fun DelegationsRepository.getDelegatesMetadataOrEmpty(chain: Chain): List<DelegateMetadata> {
    return runCatching { getDelegatesMetadata(chain) }
        .onFailure { Log.e(LOG_TAG, "Failed to fetch delegate metadatas", it) }
        .getOrDefault(emptyList())
}

suspend fun DelegationsRepository.getDelegateMetadataOrNull(chain: Chain, delegate: AccountId): DelegateMetadata? {
    return runCatching { getDelegateMetadata(chain, delegate) }
        .onFailure { Log.e(LOG_TAG, "Failed to fetch delegate metadata", it) }
        .getOrNull()
}
