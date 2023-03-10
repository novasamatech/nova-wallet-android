package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class YourDelegationsViewModel(
    private val interactor: DelegateListInteractor,
    private val governanceSharedState: GovernanceSharedState,
    private val delegateMappers: DelegateMappers,
    private val router: GovernanceRouter
) : BaseViewModel() {

    private val delegatesFlow = governanceSharedState.selectedOption
        .withLoadingShared { interactor.getUserDelegates(it, this) }
        .mapLoading { interactor.applySorting(DelegateSorting.DELEGATIONS, it) }

    val delegateModels = delegatesFlow.mapLoading { delegates ->
        val chainWithAsset = governanceSharedState.chainAndAsset()
        delegates.map { delegateMappers.mapDelegatePreviewToUi(it, chainWithAsset) }
    }.shareWhileSubscribed()

    fun delegateClicked(position: Int) = launch {
        val delegate = delegateModels.first().dataOrNull?.getOrNull(position) ?: return@launch

        val payload = DelegateDetailsPayload(delegate.accountId)
        router.openDelegateDetails(payload)
    }

    fun backClicked() {
        router.back()
    }

    fun addDelegationClicked() {
        router.openAddDelegation()
    }
}
