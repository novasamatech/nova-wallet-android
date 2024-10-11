package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView
import io.novafoundation.nova.feature_dapp_impl.presentation.search.model.DappSearchModel

class SearchDappAdapter(
    private val handler: Handler
) : GroupedListAdapter<TextHeader, DappSearchModel>(DiffCallback) {

    interface Handler {

        fun itemClicked(searchResult: DappSearchResult)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return CategoryHolder(parent)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return SearchHolder(DAppView.createUsingMathParentWidth(parent.context), handler)
    }

    override fun bindGroup(holder: GroupedListHolder, group: TextHeader) {
        (holder as CategoryHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: DappSearchModel) {
        (holder as SearchHolder).bind(child)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: DappSearchModel, payloads: List<Any>) {
        resolvePayload(holder, position, payloads) {
            (holder as SearchHolder).rebind(child) {
                when (it) {
                    DappSearchModel::title -> bindTitle(child)
                }
            }
        }
    }
}

private object DiffCallback : BaseGroupedDiffCallback<TextHeader, DappSearchModel>(TextHeader::class.java) {

    override fun areGroupItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
    }

    override fun areGroupContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
    }

    override fun areChildItemsTheSame(oldItem: DappSearchModel, newItem: DappSearchModel): Boolean {
        return when {
            isSingletonItem(oldItem) && isSingletonItem(newItem) -> true
            else -> oldItem.title == newItem.title
        }
    }

    override fun areChildContentsTheSame(oldItem: DappSearchModel, newItem: DappSearchModel): Boolean {
        return oldItem.title == newItem.title && oldItem.description == newItem.description && oldItem.icon == newItem.icon
    }

    override fun getChildChangePayload(oldItem: DappSearchModel, newItem: DappSearchModel): Any? {
        return SearchDappPayloadGenerator.diff(oldItem, newItem)
    }

    private fun isSingletonItem(item: DappSearchModel) = when (item.searchResult) {
        is DappSearchResult.Search -> true
        is DappSearchResult.Url -> true
        is DappSearchResult.Dapp -> false
    }
}

private object SearchDappPayloadGenerator : PayloadGenerator<DappSearchModel>(DappSearchModel::title)

private class CategoryHolder(parentView: ViewGroup) : GroupedListHolder(parentView.inflateChild(R.layout.item_dapp_search_category)) {

    fun bind(item: TextHeader) {
        containerView.searchCategory.text = item.content
    }
}

private class SearchHolder(
    private val dAppView: DAppView,
    private val itemHandler: SearchDappAdapter.Handler
) : GroupedListHolder(dAppView) {

    override fun unbind() {
        dAppView.clearIcon()
    }

    fun bind(item: DappSearchModel) = with(dAppView) {
        setIconUrl(item.icon)

        bindTitle(item)
        setSubtitle(item.description)
        showSubtitle(item.description != null)

        setActionResource(item.actionIcon)

        bindClick(item)
    }

    fun bindTitle(item: DappSearchModel) {
        dAppView.setTitle(item.title)
    }

    fun rebind(item: DappSearchModel, action: SearchHolder.() -> Unit) = with(containerView) {
        bindClick(item)

        action()
    }

    private fun bindClick(item: DappSearchModel) = with(dAppView) {
        setOnClickListener { itemHandler.itemClicked(item.searchResult) }
    }
}
