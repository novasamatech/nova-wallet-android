package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import kotlinx.coroutines.flow.Flow

interface DelegateListInteractor {

    fun shouldShowDelegationBanner(): Flow<Boolean>

    fun hideDelegationBanner()

    suspend fun getDelegates(
        governanceOption: SupportedGovernanceOption,
    ): Result<List<DelegatePreview>>

    suspend fun getDelegatedDelegates(
        governanceOption: SupportedGovernanceOption,
    ): Result<List<DelegatePreview>>

    suspend fun applySortingAndFiltering(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        delegates: List<DelegatePreview>
    ): List<DelegatePreview>

    suspend fun applySorting(
        sorting: DelegateSorting,
        delegates: List<DelegatePreview>
    ): List<DelegatePreview>
}
