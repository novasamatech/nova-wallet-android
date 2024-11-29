package io.novafoundation.nova.feature_swap_impl.presentation.route.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_swap_impl.R
import kotlinx.android.synthetic.main.item_route_header.view.swapRouteBack

class SwapRouteHeaderAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<SwapRouteHeaderViewHolder>() {

    interface Handler {

        fun backClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwapRouteHeaderViewHolder {
        return SwapRouteHeaderViewHolder(parent, handler)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: SwapRouteHeaderViewHolder, position: Int) {}
}

class SwapRouteHeaderViewHolder(
    parentView: ViewGroup,
    handler: SwapRouteHeaderAdapter.Handler
) : RecyclerView.ViewHolder(parentView.inflateChild(R.layout.item_route_header)) {

    init {
        itemView.swapRouteBack.setHomeButtonListener { handler.backClicked() }
    }
}
