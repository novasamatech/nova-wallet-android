package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.item_select_chain.view.itemSelectChainIcon
import kotlinx.android.synthetic.main.item_select_chain.view.itemSelectChainName

class SelectChainAdapter(
    private val imageLoader: ImageLoader,
    private val handler: ItemHandler,
    private val isEthereumBased: Boolean
) : BaseListAdapter<ChainUi, SelectChainHolder>(DiffCallback()) {

    interface ItemHandler {

        fun itemClicked(position: Int, isEthereumBased: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectChainHolder {
        return SelectChainHolder(
            containerView = parent.inflateChild(R.layout.item_select_chain),
            itemHandler = handler,
            imageLoader = imageLoader,
            isEthereumBased = isEthereumBased
        )
    }

    override fun onBindViewHolder(holder: SelectChainHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SelectChainHolder(
    containerView: View,
    private val itemHandler: SelectChainAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
    private val isEthereumBased: Boolean
) : BaseViewHolder(containerView) {

    init {
        containerView.setOnClickListener {
            itemHandler.itemClicked(bindingAdapterPosition, isEthereumBased)
        }
    }

    fun bind(item: ChainUi) = with(containerView) {
        itemSelectChainIcon.loadChainIcon(item.icon, imageLoader)
        itemSelectChainName.text = item.name
    }

    override fun unbind() {
        containerView.itemSelectChainIcon.clear()
    }
}

private class DiffCallback : DiffUtil.ItemCallback<ChainUi>() {

    override fun areItemsTheSame(oldItem: ChainUi, newItem: ChainUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChainUi, newItem: ChainUi): Boolean {
        return oldItem == newItem
    }
}
