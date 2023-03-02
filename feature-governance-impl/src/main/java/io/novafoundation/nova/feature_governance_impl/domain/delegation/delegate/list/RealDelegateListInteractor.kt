package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.list

import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.delegateComparator
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.hasMetadata
import io.novafoundation.nova.feature_governance_impl.data.repository.DelegationBannerRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapDelegateStatsToPreviews
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.repository.DelegateCommonRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealDelegateListInteractor(
    private val delegateCommonRepository: DelegateCommonRepository,
    private val chainStateRepository: ChainStateRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val delegationBannerService: DelegationBannerRepository
) : DelegateListInteractor {

    override fun shouldShowDelegationBanner(): Flow<Boolean> {
        return delegationBannerService.shouldShowBannerFlow()
    }

    override fun hideDelegationBanner() {
        delegationBannerService.hideBanner()
    }

    override suspend fun getDelegates(
        governanceOption: SupportedGovernanceOption,
    ): Flow<List<DelegatePreview>> = coroutineScope {
        val chain = governanceOption.assetWithChain.chain
        val delegateMetadataDeferred = async { delegateCommonRepository.getMetadata(governanceOption) }
        val delegatesStatsDeferred = async { delegateCommonRepository.getDelegatesStats(governanceOption) }
        val tracksDeferred = async { delegateCommonRepository.getTracks(governanceOption) }

        chainStateRepository.currentBlockNumberFlow(chain.id).map {
            val userDelegations = delegateCommonRepository.getUserDelegationsOrEmpty(governanceOption, tracksDeferred.await())
            val userDelegationIds = userDelegations.keys.map { it.value }

            val identities = identityRepository.getIdentitiesFromIds(userDelegationIds, chain.id)

            mapDelegateStatsToPreviews(
                delegatesStatsDeferred.await(),
                delegateMetadataDeferred.await(),
                identities,
                userDelegations
            )
        }
    }

    override suspend fun getUserDelegates(governanceOption: SupportedGovernanceOption): Flow<List<DelegatePreview>> = coroutineScope {
        val chain = governanceOption.assetWithChain.chain
        val delegateMetadataDeferred = async { delegateCommonRepository.getMetadata(governanceOption) }
        val tracksDeferred = async { delegateCommonRepository.getTracks(governanceOption) }
        var delegatesStats: List<DelegateStats>? = null

        chainStateRepository.currentBlockNumberFlow(chain.id).map {
            val userDelegations = delegateCommonRepository.getUserDelegationsOrEmpty(governanceOption, tracksDeferred.await())
            val userDelegationIds = userDelegations.keys.map { it.value }

            if (delegatesStats == null) {
                delegatesStats = delegateCommonRepository.getDelegatesStats(governanceOption, userDelegationIds)
            }

            val identities = identityRepository.getIdentitiesFromIds(userDelegationIds, chain.id)

            mapDelegateStatsToPreviews(
                delegatesStats!!,
                delegateMetadataDeferred.await(),
                identities,
                userDelegations
            )
        }
    }

    override suspend fun applySortingAndFiltering(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        delegates: List<DelegatePreview>
    ): List<DelegatePreview> {
        val filteredDelegates = delegates.applyFilter(filtering)
        return applySorting(sorting, filteredDelegates)
    }

    override suspend fun applySorting(
        sorting: DelegateSorting,
        delegates: List<DelegatePreview>
    ): List<DelegatePreview> {
        val comparator = getMetadataComparator()
            .thenComparing(sorting.delegateComparator())

        return delegates.sortedWith(comparator)
    }

    private fun getMetadataComparator(): Comparator<DelegatePreview> {
        return compareByDescending { it.hasMetadata() }
    }
}
