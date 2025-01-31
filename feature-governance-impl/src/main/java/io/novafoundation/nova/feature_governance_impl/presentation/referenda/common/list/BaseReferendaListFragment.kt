package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list

import androidx.viewbinding.ViewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.list.EditablePlaceholderAdapter
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListAdapter
import kotlinx.coroutines.flow.Flow

abstract class BaseReferendaListFragment<V : BaseViewModel, B : ViewBinding> : BaseFragment<V, B>(), ReferendaListAdapter.Handler {

    protected open val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { CustomPlaceholderAdapter(R.layout.item_referenda_shimmering) }
    protected open val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { EditablePlaceholderAdapter() }
    protected val referendaListAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListAdapter(this) }

    protected fun Flow<ExtendedLoadingState<ReferendaListStateModel>>.observeReferendaList() {
        observeWhenVisible { loadingState ->
            when (loadingState) {
                is ExtendedLoadingState.Loaded -> {
                    shimmeringAdapter.show(false)
                    submitReferenda(loadingState.data.referenda)
                    placeholderAdapter.show(loadingState.data.placeholderModel != null)
                    loadingState.data.placeholderModel?.let { placeholderAdapter.setPlaceholderData(it) }
                }
                is ExtendedLoadingState.Loading, is ExtendedLoadingState.Error -> {
                    shimmeringAdapter.show(true)
                    submitReferenda(emptyList())
                }
            }
        }
    }

    protected open fun submitReferenda(data: List<Any>) {
        referendaListAdapter.submitList(data)
    }
}
