package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getMaskedRipple
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.ItemNetworkSettingsBinding

class NetworkManagementListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : ListAdapter<NetworkListRvItem, NetworkListViewHolder>(NetworkManagementListDiffCallback()) {

    interface ItemHandler {

        fun onNetworkClicked(chainId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkListViewHolder {
        return NetworkListViewHolder(ItemNetworkSettingsBinding.inflate(parent.inflater(), parent, false), imageLoader, itemHandler)
    }

    override fun onBindViewHolder(holder: NetworkListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class NetworkManagementListDiffCallback : DiffUtil.ItemCallback<NetworkListRvItem>() {

    override fun areItemsTheSame(oldItem: NetworkListRvItem, newItem: NetworkListRvItem): Boolean {
        return oldItem.chainId == newItem.chainId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: NetworkListRvItem, newItem: NetworkListRvItem): Boolean {
        return oldItem == newItem
    }
}

class NetworkListViewHolder(
    private val binder: ItemNetworkSettingsBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: NetworkManagementListAdapter.ItemHandler
) : ViewHolder(binder.root) {

    init {
        itemView.background = itemView.context.getMaskedRipple(cornerSizeInDp = 0)
    }

    fun bind(item: NetworkListRvItem) = with(binder) {
        itemView.setOnClickListener { itemHandler.onNetworkClicked(item.chainId) }

        itemNetworkImage.setIcon(item.chainIcon, imageLoader)
        itemNetworkTitle.text = item.title
        itemNetworkSubtitle.setTextOrHide(item.subtitle)
        itemNetworkLabel.setTextOrHide(item.chainLabel)

        itemNetworkStatusShimmer.isVisible = item.status != null
        if (item.status != null) {
            itemNetworkStatus.setText(item.status.name)
            item.status.chainStatusColor?.let { itemNetworkStatus.setTextColor(it) }
            itemNetworkStatus.setDrawableStart(item.status.chainStatusIcon, paddingInDp = 6)
            itemNetworkStatus.setCompoundDrawableTint(item.status.chainStatusIconColor)
        }

        if (item.disabled) {
            itemNetworkImage.alpha = 0.32f
            itemNetworkTitle.setTextColorRes(R.color.text_secondary)
        } else {
            itemNetworkImage.alpha = 1f
            itemNetworkTitle.setTextColorRes(R.color.text_primary)
        }
    }
}
