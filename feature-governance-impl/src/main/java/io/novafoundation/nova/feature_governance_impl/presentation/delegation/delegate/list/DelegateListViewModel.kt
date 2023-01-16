package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.utils.withLoadingResult
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateSorting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.model.DelegateListModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DelegateListViewModel(
    private val interactor: DelegateListInteractor,
    private val governanceSharedState: GovernanceSharedState,
    private val delegateMappers: DelegateMappers,
    private val router: GovernanceRouter
) : BaseViewModel() {

    private val sorting = MutableStateFlow(DelegateSorting.DELEGATIONS)
    private val filtering = MutableStateFlow(DelegateFiltering.ALL_ACCOUNTS)

    private val delegateQueryInputs = combine(sorting, filtering, governanceSharedState.selectedOption, ::Triple)

    private val delegates = delegateQueryInputs.withLoadingResult { (sorting, filtering, selectedGovernance) ->
        interactor.getDelegates(sorting, filtering, selectedGovernance)
    }.shareInBackground()

    val delegateModels = delegates.mapLoading { delegates ->
        val governanceOption = governanceSharedState.selectedOption.first()

        delegates.map { mapDelegatePreviewToUi(it, governanceOption) }
    }.shareInBackground()

    private suspend fun mapDelegatePreviewToUi(delegatePreview: DelegatePreview, governanceOption: SupportedGovernanceOption): DelegateListModel {
        return DelegateListModel(
            icon = delegateMappers.mapDelegateIconToUi(delegatePreview),
            accountId = delegatePreview.accountId,
            name = delegateMappers.formatDelegateName(delegatePreview, governanceOption.assetWithChain.chain),
            type = delegateMappers.mapDelegateTypeToUi(delegatePreview.metadata?.accountType),
            description = delegatePreview.metadata?.shortDescription,
            stats = delegateMappers.formatDelegationStats(delegatePreview.stats, governanceOption.assetWithChain.asset)
        )
    }

    fun delegateClicked(position: Int) = launch {
        val delegate = delegateModels.first().dataOrNull?.getOrNull(position) ?: return@launch

        showMessage("TODO - clicked ${delegate.name}")
    }

    fun backClicked() {
       router.back()
    }
}
