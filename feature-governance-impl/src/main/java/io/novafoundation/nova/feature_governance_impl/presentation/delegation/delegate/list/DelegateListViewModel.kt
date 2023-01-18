package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withLoadingResult
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.common.view.input.chooser.createFromEnum
import io.novafoundation.nova.common.view.input.chooser.selectedValue
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegatePreview
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateSorting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.model.DelegateListModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DelegateListViewModel(
    private val interactor: DelegateListInteractor,
    private val governanceSharedState: GovernanceSharedState,
    private val delegateMappers: DelegateMappers,
    private val listChooserMixinFactory: ListChooserMixin.Factory,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter
) : BaseViewModel() {

    val sortingMixin = listChooserMixinFactory.createFromEnum(
        coroutineScope = viewModelScope,
        displayOf = ::sortingDisplay,
        initial = DelegateSorting.DELEGATIONS,
        selectorTitleRes = R.string.common_sort_by
    )

    val filteringMixin = listChooserMixinFactory.createFromEnum(
        coroutineScope = viewModelScope,
        displayOf = ::filteringDisplay,
        initial = DelegateFiltering.ALL_ACCOUNTS,
        selectorTitleRes = R.string.wallet_filters_header
    )

    private val delegateQueryInputs = combine(
        sortingMixin.selectedValue,
        filteringMixin.selectedValue,
        governanceSharedState.selectedOption,
        ::Triple
    )

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

    private suspend fun sortingDisplay(sorting: DelegateSorting): String {
        return when (sorting) {
            DelegateSorting.DELEGATIONS -> resourceManager.getString(R.string.delegation_delegations)
            DelegateSorting.DELEGATED_VOTES -> resourceManager.getString(R.string.delegation_delegated_votes)
            DelegateSorting.VOTING_ACTIVITY -> delegateMappers.formattedRecentVotesPeriod()
        }
    }

    private fun filteringDisplay(filtering: DelegateFiltering): String {
        val resourceId = when (filtering) {
            DelegateFiltering.ALL_ACCOUNTS -> R.string.delegation_delegate_filter_all
            DelegateFiltering.ORGANIZATIONS -> R.string.delegation_delegate_filter_organizations
            DelegateFiltering.INDIVIDUALS -> R.string.delegation_delegate_filter_individuals
        }

        return resourceManager.getString(resourceId)
    }
}
