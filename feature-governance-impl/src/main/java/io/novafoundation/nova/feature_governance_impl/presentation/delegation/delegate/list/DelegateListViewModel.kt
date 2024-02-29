package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.common.view.input.chooser.createFromEnum
import io.novafoundation.nova.common.view.input.chooser.selectedValue
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.DelegateListInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateFiltering
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.list.model.DelegateSorting
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DelegateListViewModel(
    private val interactor: DelegateListInteractor,
    private val governanceSharedState: GovernanceSharedState,
    private val delegateMappers: DelegateMappers,
    private val listChooserMixinFactory: ListChooserMixin.Factory,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
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

    val shouldShowBannerFlow = interactor.shouldShowDelegationBanner()
        .shareInBackground()

    private val delegatesFlow = governanceSharedState.selectedOption
        .withLoadingShared { interactor.getDelegates(it, this) }

    private val sortedAndFilteredDelegates = combine(
        sortingMixin.selectedValue,
        filteringMixin.selectedValue,
        delegatesFlow
    ) { sorting, filtering, delegates ->
        delegates.map { interactor.applySortingAndFiltering(sorting, filtering, it) }
    }

    val delegateModels = sortedAndFilteredDelegates.mapLoading { delegates ->
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

    private suspend fun sortingDisplay(sorting: DelegateSorting): String {
        return when (sorting) {
            DelegateSorting.DELEGATIONS -> resourceManager.getString(R.string.delegation_sorting_delegations)
            DelegateSorting.DELEGATED_VOTES -> resourceManager.getString(R.string.delegation_sorting_delegated_votes)
            DelegateSorting.VOTING_ACTIVITY -> delegateMappers.formattedRecentVotesPeriod(R.string.delegation_sorting_recent_votes_format)
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

    fun openBecomingDelegateTutorial() {
        router.openBecomingDelegateTutorial()
    }

    fun openSearch() {
        router.openDelegateSearch()
    }

    fun closeBanner() = launch {
        interactor.hideDelegationBanner()
    }
}
