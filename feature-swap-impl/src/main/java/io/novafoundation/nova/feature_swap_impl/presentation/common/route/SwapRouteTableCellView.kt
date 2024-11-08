package io.novafoundation.nova.feature_swap_impl.presentation.common.route

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.GenericTableCellView
import io.novafoundation.nova.feature_swap_impl.R

class SwapRouteTableCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : GenericTableCellView<SwapRouteView>(context, attrs, defStyle, defStyleRes) {

    init {
        setValueView(SwapRouteView(context))
        setTitle(R.string.swap_route)
    }

    fun setSwapRouteState(routeState: SwapRouteState) {
        setVisible(!routeState.isError)

        showProgress(routeState.isLoading)

        routeState.onLoaded { routeModel ->
            setVisible(routeModel != null)

            routeModel?.let(valueView::setModel)
        }
    }

    fun setShowChainNames(showChainNames: Boolean) {
        valueView.setShowChainNames(showChainNames)
    }

    fun setSwapRouteModel(model: SwapRouteModel) {
        setVisible(true)
        valueView.setModel(model)
    }
}
