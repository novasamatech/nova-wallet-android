package io.novafoundation.nova.feature_swap_impl.presentation.route

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.common.mixin.impl.observeRetries
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.domain.onNotLoaded
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getTopRoundedCornerDrawable
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentRouteBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.SwapRouteAdapter
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.SwapRouteHeaderAdapter
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.SwapRouteViewHolder
import io.novafoundation.nova.feature_swap_impl.presentation.route.list.TimelineItemDecoration

class SwapRouteFragment : BaseBottomSheetFragment<SwapRouteViewModel, FragmentRouteBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "swap_operation"

        fun getBundle(payload: SwapOperationPayload) = Bundle().apply { putParcelable(KEY_PAYLOAD, payload) }
    }

    override fun createBinding() = FragmentRouteBinding.inflate(layoutInflater)

    private lateinit var headerAdapter: SwapRouteHeaderAdapter
    private lateinit var routeAdapter: SwapRouteAdapter

    override fun initViews() {
        binder.swapRouteContent.itemAnimator = null

        val timelineDecoration = TimelineItemDecoration(
            context = requireContext(),
            shouldDecorate = { it is SwapRouteViewHolder }
        )
        binder.swapRouteContent.addItemDecoration(timelineDecoration)

        headerAdapter = SwapRouteHeaderAdapter()
        routeAdapter = SwapRouteAdapter()

        binder.swapRouteContent.adapter = ConcatAdapter(headerAdapter, routeAdapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapRoute()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SwapRouteViewModel) {
        observeRetries(viewModel)

        viewModel.swapRoute.observe { routeState ->
            routeState.onLoaded {
                binder.swapRouteProgress.makeGone()
                routeAdapter.submitList(it)
            }.onNotLoaded {
                binder.swapRouteProgress.makeVisible()
                routeAdapter.submitList(emptyList())
            }
        }
    }
}
