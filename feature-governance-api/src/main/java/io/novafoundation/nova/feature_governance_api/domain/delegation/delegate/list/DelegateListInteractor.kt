package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list

import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting

interface DelegateListInteractor {

    suspend fun getDelegates(
        sorting: DelegateSorting,
        filtering: DelegateFiltering,
        governanceOption: SupportedGovernanceOption,
    ): Result<List<DelegatePreview>>
}
