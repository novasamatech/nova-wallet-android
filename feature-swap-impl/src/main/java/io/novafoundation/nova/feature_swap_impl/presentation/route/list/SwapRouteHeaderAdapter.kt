package io.novafoundation.nova.feature_swap_impl.presentation.route.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_swap_impl.databinding.ItemRouteHeaderBinding

class SwapRouteHeaderAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<SwapRouteHeaderViewHolder>() {

    interface Handler {

        fun backClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwapRouteHeaderViewHolder {
        return SwapRouteHeaderViewHolder(ItemRouteHeaderBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: SwapRouteHeaderViewHolder, position: Int) {}
}

class SwapRouteHeaderViewHolder(
    binder: ItemRouteHeaderBinding,
    handler: SwapRouteHeaderAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    init {
        binder.swapRouteBack.setHomeButtonListener { handler.backClicked() }
    }
}
