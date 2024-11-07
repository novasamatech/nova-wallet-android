package io.novafoundation.nova.feature_swap_impl.presentation.route.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.route.model.SwapRouteItemModel
import kotlinx.android.synthetic.main.item_route_swap.view.itemRouteSwapAmountFrom
import kotlinx.android.synthetic.main.item_route_swap.view.itemRouteSwapAmountTo
import kotlinx.android.synthetic.main.item_route_swap.view.itemRouteSwapChain
import kotlinx.android.synthetic.main.item_route_swap.view.itemRouteSwapFee
import kotlinx.android.synthetic.main.item_route_transfer.view.itemRouteTransferAmount
import kotlinx.android.synthetic.main.item_route_transfer.view.itemRouteTransferFee
import kotlinx.android.synthetic.main.item_route_transfer.view.itemRouteTransferFrom
import kotlinx.android.synthetic.main.item_route_transfer.view.itemRouteTransferTo

class SwapRouteAdapter : BaseListAdapter<SwapRouteItemModel, SwapRouteViewHolder>(SwapRouteDiffCallback()) {

    override fun onBindViewHolder(holder: SwapRouteViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SwapRouteItemModel.Swap -> (holder as SwapRouteSwapViewHolder).bind(item)
            is SwapRouteItemModel.Transfer -> (holder as SwapRouteTransferViewHolder).bind(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwapRouteViewHolder {
        return when (viewType) {
            R.layout.item_route_swap -> SwapRouteSwapViewHolder(parent)
            R.layout.item_route_transfer -> SwapRouteTransferViewHolder(parent)
            else -> error("Unknown viewType: $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SwapRouteItemModel.Swap -> R.layout.item_route_swap
            is SwapRouteItemModel.Transfer -> R.layout.item_route_transfer
        }
    }
}

sealed class SwapRouteViewHolder(itemView: View) : BaseViewHolder(itemView)  {

    init {
        with(itemView) {
            background = context.getRoundedCornerDrawable(R.color.input_background)
        }
    }
}

class SwapRouteTransferViewHolder(parentView: ViewGroup) : SwapRouteViewHolder(parentView.inflateChild(R.layout.item_route_transfer)) {


    fun bind(model: SwapRouteItemModel.Transfer) = with(containerView) {
        itemRouteTransferAmount.setModel(model.amount)
        itemRouteTransferFee.text = model.fee
        itemRouteTransferFrom.text = model.originChainName
        itemRouteTransferTo.text = model.destinationChainName
    }

    override fun unbind() {}
}

class SwapRouteSwapViewHolder(parentView: ViewGroup) : SwapRouteViewHolder(parentView.inflateChild(R.layout.item_route_swap)) {


    fun bind(model: SwapRouteItemModel.Swap)  = with(containerView) {
        itemRouteSwapAmountFrom.setModel(model.amountFrom)
        itemRouteSwapAmountTo.setModel(model.amountTo)
        itemRouteSwapFee.text = model.fee
        itemRouteSwapChain.text = model.chain
    }

    override fun unbind() {}
}

private class SwapRouteDiffCallback : DiffUtil.ItemCallback<SwapRouteItemModel>() {

    override fun areItemsTheSame(oldItem: SwapRouteItemModel, newItem: SwapRouteItemModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SwapRouteItemModel, newItem: SwapRouteItemModel): Boolean {
        return oldItem == newItem
    }
}
