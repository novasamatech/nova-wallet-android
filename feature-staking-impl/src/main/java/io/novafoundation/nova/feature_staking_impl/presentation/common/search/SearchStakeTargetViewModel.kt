package io.novafoundation.nova.feature_staking_impl.presentation.common.search

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.SearchState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

abstract class SearchStakeTargetViewModel<S>(protected val resourceManager: ResourceManager) : BaseViewModel() {

    protected abstract val dataFlow: Flow<List<StakeTargetModel<S>>?>

    // flow wrapper is needed to avoid calling dataFlow before child constructor has been finished
    val screenState = flow {
        val innerFlow = dataFlow.map { data ->
            when {
                data == null -> SearchState.NoInput

                data.isNullOrEmpty().not() -> {
                    SearchState.Success(
                        data = data,
                        headerTitle = resourceManager.getString(R.string.common_search_results_number, data.size)
                    )
                }

                else -> SearchState.NoResults
            }
        }

        emitAll(innerFlow)
    }
        .onStart { emit(SearchState.Loading) }
        .shareInBackground(SharingStarted.Lazily)

    val enteredQuery = MutableStateFlow("")

    abstract fun itemClicked(item: StakeTargetModel<S>)

    abstract fun itemInfoClicked(item: StakeTargetModel<S>)

    abstract fun backClicked()
}
