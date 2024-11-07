package io.novafoundation.nova.feature_swap_impl.presentation.route

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.domain.onNotLoaded
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.SwapRouteAdapter
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.SwapRouteHeaderAdapter
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.SwapRouteViewHolder
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.TimelineItemDecoration
import kotlinx.android.synthetic.main.fragment_route.swapRouteContent
import kotlinx.android.synthetic.main.fragment_route.swapRouteProgress

class SwapRouteFragment : BaseFragment<SwapRouteViewModel>(), SwapRouteHeaderAdapter.Handler {

    private lateinit var headerAdapter: SwapRouteHeaderAdapter
    private lateinit var routeAdapter: SwapRouteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_route, container, false)
    }

    override fun initViews() {
        swapRouteContent.applyStatusBarInsets()
        swapRouteContent.setHasFixedSize(true)
        swapRouteContent.itemAnimator = null

        val timelineDecoration = TimelineItemDecoration(
            context = requireContext(),
            shouldDecorate = { it is SwapRouteViewHolder }
        )
        swapRouteContent.addItemDecoration(timelineDecoration)

        headerAdapter = SwapRouteHeaderAdapter(this)
        routeAdapter = SwapRouteAdapter()

        swapRouteContent.adapter = ConcatAdapter(headerAdapter, routeAdapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapRoute()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapRouteViewModel) {
        viewModel.swapRoute.observe { routeState ->
            routeState.onLoaded {
                swapRouteProgress.makeGone()
                routeAdapter.submitList(it)
            }.onNotLoaded {
                swapRouteProgress.makeVisible()
                routeAdapter.submitList(emptyList())
            }
        }
    }

    override fun backClicked() {
        viewModel.backClicked()
    }
}
