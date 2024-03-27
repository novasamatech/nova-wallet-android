package io.novafoundation.nova.feature_push_notifications.presentation.staking.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIconToTarget
import io.novafoundation.nova.feature_push_notifications.R
import kotlinx.android.synthetic.main.item_push_staking_settings.view.pushStakingItem

class PushStakingSettingsAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : ListAdapter<PushStakingRVItem, PushStakingItemViewHolder>(PushStakingItemCallback()) {

    interface ItemHandler {
        fun itemClicked(item: PushStakingRVItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PushStakingItemViewHolder {
        return PushStakingItemViewHolder(parent.inflateChild(R.layout.item_push_staking_settings), imageLoader, itemHandler)
    }

    override fun onBindViewHolder(holder: PushStakingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: PushStakingItemViewHolder, position: Int, payloads: MutableList<Any>) {
        resolvePayload(holder, position, payloads) {
            val item = getItem(position)

            when (it) {
                PushStakingRVItem::isEnabled -> holder.setEnabled(item)
            }
        }
    }
}

class PushStakingItemCallback() : DiffUtil.ItemCallback<PushStakingRVItem>() {
    override fun areItemsTheSame(oldItem: PushStakingRVItem, newItem: PushStakingRVItem): Boolean {
        return oldItem.chainId == newItem.chainId
    }

    override fun areContentsTheSame(oldItem: PushStakingRVItem, newItem: PushStakingRVItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: PushStakingRVItem, newItem: PushStakingRVItem): Any? {
        return PushStakingPayloadGenerator.diff(oldItem, newItem)
    }
}

class PushStakingItemViewHolder(
    itemView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: PushStakingSettingsAdapter.ItemHandler
) : ViewHolder(itemView) {

    init {
        itemView.pushStakingItem.setIconTintColor(null)
    }

    fun bind(item: PushStakingRVItem) {
        with(itemView) {
            pushStakingItem.setOnClickListener {
                itemHandler.itemClicked(item)
            }

            pushStakingItem.setTitle(item.chainName)
            imageLoader.loadChainIconToTarget(item.chainIconUrl, context) {
                pushStakingItem.setIcon(it)
            }

            setEnabled(item)
        }
    }

    fun setEnabled(item: PushStakingRVItem) {
        itemView.pushStakingItem.setChecked(item.isEnabled)
    }
}

private object PushStakingPayloadGenerator : PayloadGenerator<PushStakingRVItem>(
    PushStakingRVItem::isEnabled
)
