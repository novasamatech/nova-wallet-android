package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.search

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceAdditionalState
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegatePreview
import io.novafoundation.nova.runtime.state.GenericSingleAssetSharedState
import kotlinx.coroutines.flow.Flow

class DelegateSearchResult(
    val delegates: List<DelegatePreview>,
    val query: String
)

interface DelegateSearchInteractor {

    suspend fun searchDelegates(
        queryFlow: Flow<String>,
        selectedOption: GenericSingleAssetSharedState.SupportedAssetOption<GovernanceAdditionalState>
    ): Flow<ExtendedLoadingState<List<DelegatePreview>>>
}
