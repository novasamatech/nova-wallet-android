package io.novafoundation.nova.feature_currency_impl.presentation.currency

import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_currency_api.presentation.model.CurrencyModel
import io.novafoundation.nova.feature_currency_impl.R
import io.novafoundation.nova.feature_currency_impl.databinding.ItemCurrencyBinding
import io.novafoundation.nova.feature_currency_impl.databinding.ItemCurrencyTypeBinding

class CurrencyAdapter(
    private val handler: Handler
) : GroupedListAdapter<TextHeader, CurrencyModel>(DiffCallback) {

    interface Handler {
        fun itemClicked(currency: CurrencyModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return CurrencyTypeHolder(ItemCurrencyTypeBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return CurrencyHolder(ItemCurrencyBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun bindGroup(holder: GroupedListHolder, group: TextHeader) {
        (holder as CurrencyTypeHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: CurrencyModel) {
        (holder as CurrencyHolder).bind(child)
    }
}

private object DiffCallback : BaseGroupedDiffCallback<TextHeader, CurrencyModel>(TextHeader::class.java) {

    override fun areGroupItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
    }

    override fun areGroupContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
    }

    override fun areChildItemsTheSame(oldItem: CurrencyModel, newItem: CurrencyModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: CurrencyModel, newItem: CurrencyModel): Boolean {
        return oldItem == newItem
    }
}

private class CurrencyTypeHolder(private val binder: ItemCurrencyTypeBinding) : GroupedListHolder(binder.root) {

    fun bind(item: TextHeader) {
        binder.itemCurrencyType.text = item.content
    }
}

private class CurrencyHolder(
    private val binder: ItemCurrencyBinding,
    private val itemHandler: CurrencyAdapter.Handler
) : GroupedListHolder(binder.root) {

    fun bind(item: CurrencyModel) = with(binder) {
        itemCurrencySign.text = item.displayCode

        bindTitle(item)
        itemCurrencyAbbreviation.text = item.code

        itemCurrencyName.text = item.name
        itemCurrencyCheck.isChecked = item.isSelected
        bindClick(item)
    }

    fun bindTitle(item: CurrencyModel) = with(containerView) {
        binder.itemCurrencyName.text = item.name
    }

    private fun bindClick(item: CurrencyModel) = with(containerView) {
        setOnClickListener { itemHandler.itemClicked(item) }
    }
}
