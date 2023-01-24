package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.PlaceholderAdapter
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

abstract class BaseReferendaListFragment<V : BaseViewModel> : BaseFragment<V>(), ReferendaListAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    protected val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_referenda_shimmering) }
    protected val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_referenda_placeholder) }
    protected val referendaListAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListAdapter(this, imageLoader) }

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
