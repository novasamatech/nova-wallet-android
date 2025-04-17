package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_assets.databinding.ItemSelectChainBinding

class SelectChainAdapter(
    private val imageLoader: ImageLoader,
    private val handler: ItemHandler
) : BaseListAdapter<ChainUi, SelectChainHolder>(DiffCallback()) {

    interface ItemHandler {

        fun itemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectChainHolder {
        return SelectChainHolder(
            binder = ItemSelectChainBinding.inflate(parent.inflater(), parent, false),
            itemHandler = handler,
            imageLoader = imageLoader
        )
    }

    override fun onBindViewHolder(holder: SelectChainHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SelectChainHolder(
    private val binder: ItemSelectChainBinding,
    private val itemHandler: SelectChainAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(binder.root) {

    init {
        binder.root.setOnClickListener {
            itemHandler.itemClicked(bindingAdapterPosition)
        }
    }

    fun bind(item: ChainUi) = with(binder) {
        itemSelectChainIcon.loadChainIcon(item.icon, imageLoader)
        itemSelectChainName.text = item.name
    }

    override fun unbind() {
        binder.itemSelectChainIcon.clear()
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
