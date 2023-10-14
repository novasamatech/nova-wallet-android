package io.novafoundation.nova.feature_nft_impl.presentation.nft.details

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_nft_impl.R
import kotlinx.android.synthetic.main.item_nft_tag.view.itemNftTag

class NftTagsAdapter: BaseListAdapter<String, TagNftViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagNftViewHolder {
        return TagNftViewHolder(parent)
    }

    override fun onBindViewHolder(holder: TagNftViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TagNftViewHolder(
    parent: ViewGroup
) : BaseViewHolder(parent.inflateChild(R.layout.item_nft_tag)) {
    fun bind(tag: String) {
        containerView.itemNftTag.text = tag
    }

    override fun unbind() {}
}

private class DiffCallback : DiffUtil.ItemCallback<String>() {

    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return true
    }
}
