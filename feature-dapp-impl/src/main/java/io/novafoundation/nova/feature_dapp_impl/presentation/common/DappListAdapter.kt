package io.novafoundation.nova.feature_dapp_impl.presentation.common

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.feature_dapp_impl.R
import kotlinx.android.synthetic.main.item_dapp.view.itemDAppIcon
import kotlinx.android.synthetic.main.item_dapp.view.itemDAppSubtitle
import kotlinx.android.synthetic.main.item_dapp.view.itemDAppTitle
import kotlinx.android.synthetic.main.item_dapp.view.itemDappAction

class DappListAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader,
) : BaseListAdapter<DappModel, DappViewHolder>(DappDiffCallback) {

    interface Handler {

        fun onItemClicked(item: DappModel)

        fun onItemFavouriteClicked(item: DappModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappViewHolder {
        return DappViewHolder(parent.inflateChild(R.layout.item_dapp), handler, imageLoader)
    }

    override fun onBindViewHolder(holder: DappViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DappDiffCallback : DiffUtil.ItemCallback<DappModel>() {

    override fun areItemsTheSame(oldItem: DappModel, newItem: DappModel): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: DappModel, newItem: DappModel): Boolean {
        return oldItem == newItem
    }
}

class DappViewHolder(
    containerView: View,
    private val itemHandler: DappListAdapter.Handler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(containerView) {

    fun bind(item: DappModel) = with(containerView) {
        itemDAppIcon.showDAppIcon(item.iconUrl, imageLoader)
        itemDAppTitle.text = item.name
        itemDAppSubtitle.text = item.description

        itemDappAction.isActivated = item.isFavourite
        itemDappAction.setOnClickListener { itemHandler.onItemFavouriteClicked(item) }

        if (item.isFavourite) {
            itemDappAction.setImageResource(R.drawable.ic_heart_filled)
            itemDappAction.imageTintList = null
        } else {
            itemDappAction.setImageResource(R.drawable.ic_heart_outline)
            itemDappAction.setImageTintRes(R.color.white_48)
        }

        setOnClickListener { itemHandler.onItemClicked(item) }
    }

    override fun unbind() {
        containerView.itemDAppIcon.clear()
    }
}
