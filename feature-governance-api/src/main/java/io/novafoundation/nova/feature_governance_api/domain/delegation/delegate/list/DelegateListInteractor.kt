package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateSorting

interface DelegateListInteractor {

    suspend fun getDelegates(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        governanceOption: SupportedGovernanceOption,
    ): Result<List<DelegatePreview>>
}
