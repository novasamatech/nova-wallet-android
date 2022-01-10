package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.view.ViewGroup
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_impl.presentation.search.model.DappSearchModel
import kotlinx.android.synthetic.main.item_text_header.view.searchCategory

class SearchDappAdapter : GroupedListAdapter<TextHeader, DappSearchModel>(DiffCallback) {

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return CategoryHolder(parent)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return
    }

    override fun bindGroup(holder: GroupedListHolder, group: TextHeader) {
        TODO("Not yet implemented")
    }

    override fun bindChild(holder: GroupedListHolder, child: DappSearchModel) {
        TODO("Not yet implemented")
    }
}


private object DiffCallback: BaseGroupedDiffCallback<TextHeader, DappSearchModel>(TextHeader::class.java) {

    override fun areGroupItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
    }

    override fun areGroupContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
    }

    override fun areChildItemsTheSame(oldItem: DappSearchModel, newItem: DappSearchModel): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areChildContentsTheSame(oldItem: DappSearchModel, newItem: DappSearchModel): Boolean {
        return oldItem.title == newItem.title && oldItem.description == newItem.description && oldItem.icon == newItem.icon
    }

}

private class CategoryHolder(parentView: ViewGroup) : GroupedListHolder(parentView.inflateChild(R.layout.item_dapp_search_category)) {

    fun bind(item: TextHeader) {
        containerView.searchCategory.text = item.content
    }
}
