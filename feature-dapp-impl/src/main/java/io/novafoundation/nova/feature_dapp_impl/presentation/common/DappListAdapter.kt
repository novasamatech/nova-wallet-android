package io.novafoundation.nova.feature_dapp_impl.presentation.common

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView

class DappListAdapter(
    private val handler: Handler
) : BaseListAdapter<DappModel, DappViewHolder>(DappDiffCallback) {

    interface Handler {

        fun onDAppClicked(item: DappModel)

        fun onItemFavouriteClicked(item: DappModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappViewHolder {
        return DappViewHolder(DAppView.createUsingMathParentWidth(parent.context), handler)
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
    private val dAppView: DAppView,
    private val itemHandler: DappListAdapter.Handler,
) : BaseViewHolder(dAppView) {

    fun bind(item: DappModel) = with(dAppView) {
        setTitle(item.name)
        setSubtitle(item.description)
        setIconUrl(item.iconUrl)
        activateActionIcon(item.isFavourite)
        setOnActionClickListener { itemHandler.onItemFavouriteClicked(item) }

        if (item.isFavourite) {
            setActionResource(R.drawable.ic_favorite_heart_filled_20)
            setActionTintRes(null)
        } else {
            setActionResource(R.drawable.ic_favorite_heart_outline_20)
            setActionTintRes(R.color.icon_secondary)
        }

        setOnClickListener { itemHandler.onDAppClicked(item) }
    }

    override fun unbind() {
        dAppView.clearIcon()
    }
}
