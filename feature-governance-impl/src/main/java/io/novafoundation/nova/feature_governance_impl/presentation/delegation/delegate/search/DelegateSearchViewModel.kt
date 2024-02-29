package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.search.DelegateSearchInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SearchPlaceholderModel(@StringRes val textRes: Int, @DrawableRes val drawableRes: Int)

class DelegateSearchViewModel(
    private val interactor: DelegateSearchInteractor,
    private val governanceSharedState: GovernanceSharedState,
    private val delegateMappers: DelegateMappers,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
) : BaseViewModel() {

    val query = MutableStateFlow("")

    private val searchResult = governanceSharedState.selectedOption
        .flatMapLatest { interactor.searchDelegates(query, it, this) }
        .distinctUntilChanged()
        .inBackground()
        .shareWhileSubscribed()

    val searchResultCount = searchResult
        .map {
            val delegates = it.dataOrNull
            if (delegates.isNullOrEmpty()) {
                null
            } else {
                resourceManager.getString(R.string.delegate_search_result_count, delegates.size)
            }
        }.inBackground()
        .shareWhileSubscribed()

    val searchPlaceholderModel = combine(query, searchResult) { query, searchResultLoading ->
        val delegates = searchResultLoading.dataOrNull
        when {
            searchResultLoading.isLoading() -> null
            query.isNotEmpty() && delegates != null && delegates.isEmpty() -> {
                SearchPlaceholderModel(
                    R.string.delegate_search_placeholder_empty,
                    R.drawable.ic_no_search_results
                )
            }
            delegates?.isEmpty() == true -> SearchPlaceholderModel(
                R.string.common_search_placeholder_default,
                R.drawable.ic_placeholder
            )
            else -> null
        }
    }

    val delegateModels = searchResult.mapLoading { delegates ->
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
}
