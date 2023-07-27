package io.novafoundation.nova.feature_nft_impl.presentation.nft.receive.flow

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_chain.view.itemChainIcon
import kotlinx.android.synthetic.main.item_chain.view.itemChainName
import kotlinx.android.synthetic.main.item_chain.view.root

class NftChainsAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : ListAdapter<Chain, NftChainHolder>(DiffCallback) {

    interface Handler {

        fun itemClicked(item: Chain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NftChainHolder {
        return NftChainHolder(parent.inflateChild(R.layout.item_chain), imageLoader, handler)
    }

    override fun onBindViewHolder(holder: NftChainHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: NftChainHolder) {
        holder.unbind()
    }
}

private object DiffCallback : DiffUtil.ItemCallback<Chain>() {

    override fun areItemsTheSame(oldItem: Chain, newItem: Chain): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Chain, newItem: Chain): Boolean {
        return oldItem == newItem
    }
}

class NftChainHolder(
    override val containerView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: NftChainsAdapter.Handler
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            root.background = with(context) {
                addRipple(getBlockDrawable(), getRippleMask())
            }
        }
    }

    fun unbind() = with(containerView) {
        itemChainIcon.clear()
    }

    fun bind(item: Chain) = with(containerView) {
        itemChainIcon.loadChainIcon(item.icon, imageLoader)
        itemChainName.text = item.name
        root.setOnClickListener { itemHandler.itemClicked(item) }
        root.isClickable = true
    }
}
