package io.novafoundation.nova.feature_nft_impl.presentation.nft.send.flow

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.presentation.asset.AbstractAssetGroupViewHolder
import io.novafoundation.nova.common.presentation.asset.AbstractAssetViewHolder
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.domain.nft.search.SendNftListItem
import kotlinx.android.synthetic.main.item_nft_group.view.itemGroupChain
import kotlinx.android.synthetic.main.item_nft_linear.view.itemNftCollectionName
import kotlinx.android.synthetic.main.item_nft_linear.view.itemNftImage
import kotlinx.android.synthetic.main.item_nft_linear.view.itemNftName

class NftLinearAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemAssetHandler,
) : GroupedListAdapter<ChainUi, SendNftListItem>(DiffCallback) {

    interface ItemAssetHandler {
        fun nftClicked(item: SendNftListItem)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_nft_group)

        return NftGroupViewHolder(view)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_nft_linear)

        return NftViewHolder(view, imageLoader, itemHandler)
    }

    override fun bindGroup(holder: GroupedListHolder, group: ChainUi) {
        require(holder is NftGroupViewHolder)

        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: SendNftListItem) {
        require(holder is NftViewHolder)

        holder.bind(child)
    }
}

class NftGroupViewHolder(
    containerView: View,
) : GroupedListHolder(containerView), AbstractAssetGroupViewHolder {

    fun bind(chain: ChainUi) = with(containerView) {
        itemGroupChain.setChain(chain)
    }
}

class NftViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: NftLinearAdapter.ItemAssetHandler
) : GroupedListHolder(containerView), AbstractAssetViewHolder {

    fun bind(item: SendNftListItem) = with(containerView) {
        itemNftImage.load(item.media, imageLoader) {
            transformations(RoundedCornersTransformation(6.dpF(context)))
            placeholder(R.drawable.nft_media_progress)
            error(R.drawable.nft_media_error)
            fallback(R.drawable.nft_media_error)
        }
        itemNftName.text = item.name
        itemNftCollectionName.text = item.collectionName

        setOnClickListener { itemHandler.nftClicked(item) }
    }
}

private object DiffCallback : BaseGroupedDiffCallback<ChainUi, SendNftListItem>(ChainUi::class.java) {

    override fun areGroupItemsTheSame(oldItem: ChainUi, newItem: ChainUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areGroupContentsTheSame(oldItem: ChainUi, newItem: ChainUi): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: SendNftListItem, newItem: SendNftListItem): Boolean {
        return oldItem.identifier == newItem.identifier
    }

    override fun areChildContentsTheSame(oldItem: SendNftListItem, newItem: SendNftListItem): Boolean {
        return oldItem == newItem
    }
}
