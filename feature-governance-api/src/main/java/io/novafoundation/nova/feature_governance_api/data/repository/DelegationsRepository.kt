package io.novafoundation.nova.feature_governance_api.data.repository

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface DelegationsRepository {

    suspend fun getDelegatesStats(
        recentVotesBlockThreshold: BlockNumber,
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
