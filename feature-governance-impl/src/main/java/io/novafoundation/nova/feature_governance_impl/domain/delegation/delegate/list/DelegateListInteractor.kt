package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateStats
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateSorting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateStats
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegateComparator
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.util.blockInPast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class RealDelegateListInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val identityRepository: OnChainIdentityRepository,
) : DelegateListInteractor {

    override suspend fun getDelegates(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        governanceOption: SupportedGovernanceOption,
    ): Result<List<DelegatePreview>> = withContext(Dispatchers.Default) {
        runCatching {
            getDelegatesInternal(sorting, filtering, governanceOption)
        }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun CoroutineScope.getDelegatesInternal(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        governanceOption: SupportedGovernanceOption,
    ): List<DelegatePreview> {
        val chain = governanceOption.assetWithChain.chain
        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)
        val delegationsRepository = governanceSource.delegationsRepository

        val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)
        val recentVotesBlockThreshold = blockDurationEstimator.blockInPast(RECENT_VOTES_PERIOD)

        val delegatesStatsDeferred = async { delegationsRepository.getOffChainDelegatesStats(recentVotesBlockThreshold, chain) }

        val delegateMetadatasDeferred = async {
            runCatching { delegationsRepository.getOffChainDelegatesMetadata(chain) }
                .onFailure { Log.e(LOG_TAG, "Failed to fetch delegate metadatas", it) }
                .getOrDefault(emptyList())
        }

        val delegateAccountIds = delegatesStatsDeferred.await().map(OffChainDelegateStats::accountId)
        val delegateMetadatasByAccountId = delegateMetadatasDeferred.await().associateBy { AccountIdKey(it.accountId) }

        val identities = identityRepository.getIdentitiesFromIds(delegateAccountIds, chain.id)

        val delegates = delegatesStatsDeferred.await().map { delegateStats ->
            val metadata = delegateMetadatasByAccountId[delegateStats.accountId]
            val identity = identities[delegateStats.accountId]

            DelegatePreview(
                accountId = delegateStats.accountId,
                stats = mapStatsToDomain(delegateStats),
                metadata = mapMetadataToDomain(metadata),
                onChainIdentity = identity
            )
        }

        return delegates.applyFilter(filtering)
            .sortedWith(sorting.delegateComparator())
    }

    private fun mapStatsToDomain(stats: OffChainDelegateStats): DelegateStats {
        return DelegateStats(
            delegatedVotes = stats.delegatedVotes,
            delegationsCount = stats.delegationsCount,
            recentVotes = DelegateStats.RecentVotes(
                numberOfVotes = stats.recentVotes,
            )
        )
    }

    private fun mapMetadataToDomain(metadata: OffChainDelegateMetadata?): DelegatePreview.Metadata? {
        if (metadata == null) return null

        return DelegatePreview.Metadata(
            shortDescription = metadata.shortDescription,
            accountType = mapAccountTypeToDomain(metadata.isOrganization),
            iconUrl = metadata.profileImageUrl,
            name = metadata.name
        )
    }

    private fun mapAccountTypeToDomain(isOrganization: Boolean): DelegateAccountType {
        return if (isOrganization) DelegateAccountType.ORGANIZATION else DelegateAccountType.INDIVIDUAL
    }
}
