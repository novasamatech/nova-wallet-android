package io.novafoundation.nova.feature_swap_impl.presentation.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import androidx.core.view.updateMargins
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.view.TableView
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteTableCellView
import io.novafoundation.nova.feature_swap_impl.presentation.fee.model.SwapSegmentFeeModel
import io.novafoundation.nova.feature_swap_impl.presentation.fee.model.SwapSegmentFeeModel.FeeOperationModel
import io.novafoundation.nova.feature_swap_impl.presentation.fee.model.SwapSegmentFeeModel.SwapComponentFeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView
import kotlinx.android.synthetic.main.fragment_swap_fee.swapFeeContent
import kotlinx.android.synthetic.main.fragment_swap_fee.swapFeeTotal

class SwapFeeFragment : BaseBottomSheetFragment<SwapFeeViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_fee, container, false)
    }

    override fun initViews() {}

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapFee()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapFeeViewModel) {
        viewModel.swapFeeSegments.observe { feeState ->
            feeState.onLoaded(::showFeeSegments)
        }

        viewModel.totalFee.observe(swapFeeTotal::setText)
    }

    private fun showFeeSegments(feeSegments: List<SwapSegmentFeeModel>) {
        swapFeeContent.removeAllViews()

        return feeSegments.forEachIndexed { index, swapSegmentFeeModel ->
            showFeeSegment(
                feeSegment = swapSegmentFeeModel,
                isFirst = index == 0,
                isLast = index == feeSegments.size - 1
            )
        }
    }

    private fun showFeeSegment(
        feeSegment: SwapSegmentFeeModel,
        isFirst: Boolean,
        isLast: Boolean
    ) {
        val segmentTable = TableView(requireContext()).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                updateMargins(
                    top = if (isFirst) 8.dp else 12.dp,
                    bottom = if (isLast) 8.dp else 0
                )
            }
        }

        with(segmentTable) {
            addView(createSegmentOperation(feeSegment.operation))

            feeSegment.feeComponents.forEach {
                val componentViews = createFeeComponentViews(it)
                componentViews.forEach(::addView)
            }
        }

        swapFeeContent.addView(segmentTable)
    }

    private fun createFeeComponentViews(model: SwapComponentFeeModel): List<View> {
        return model.individualFees.mapIndexed { index, feeDisplay ->
            val isFirst = index == 0
            val isLast = index == model.individualFees.size - 1

            val label = model.label.takeIf { isFirst }

            FeeView(requireContext()).apply {
                setShouldDrawDivider(isLast)
                setTitle(label)
                setFeeDisplay(feeDisplay)
            }
        }
    }

    private fun createSegmentOperation(model: FeeOperationModel): View {
        return SwapRouteTableCellView(requireContext()).apply {
            setShowChainNames(true)
            setTitle(model.label)
            setSwapRouteModel(model.swapRoute)
        }
    }
}
