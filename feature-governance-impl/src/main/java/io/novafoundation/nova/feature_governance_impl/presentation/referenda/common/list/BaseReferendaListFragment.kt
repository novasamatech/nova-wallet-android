package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.PlaceholderAdapter
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListAdapter
import kotlinx.coroutines.flow.Flow

abstract class BaseReferendaListFragment<V : BaseViewModel> : BaseFragment<V>(), ReferendaListAdapter.Handler {

    protected val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_referenda_shimmering) }
    protected val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_referenda_placeholder) }
    protected val referendaListAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListAdapter(this) }

    protected fun Flow<LoadingState<List<Any?>>>.observeReferendaList() {
        observeWhenVisible {
            when (it) {
                is LoadingState.Loaded -> {
                    shimmeringAdapter.showPlaceholder(false)
                    referendaListAdapter.submitList(it.data)
                    placeholderAdapter.showPlaceholder(it.data.isEmpty())
                }
                is LoadingState.Loading -> {
                    shimmeringAdapter.showPlaceholder(true)
                    referendaListAdapter.submitList(emptyList())
                }
            }
        }
    }
}
