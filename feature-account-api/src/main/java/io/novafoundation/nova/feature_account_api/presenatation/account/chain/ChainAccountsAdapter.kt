package io.novafoundation.nova.feature_account_api.presenatation.account.chain

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import coil.ImageLoader
import io.novafoundation.nova.common.databinding.ItemTextHeaderBinding
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.headers.TextHeaderHolder
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_api.view.ItemChainAccount

class ChainAccountsAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : GroupedListAdapter<TextHeader, AccountInChainUi>(DiffCallback) {

    interface Handler {

        fun chainAccountClicked(item: AccountInChainUi)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return TextHeaderHolder(ItemTextHeaderBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = ItemChainAccount(parent.context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        return ChainAccountHolder(view)
    }

    override fun bindGroup(holder: GroupedListHolder, group: TextHeader) {
        holder.castOrNull<TextHeaderHolder>()?.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: AccountInChainUi) {
        holder.castOrNull<ChainAccountHolder>()?.bind(child, handler, imageLoader)
    }
}

class ChainAccountHolder(override val containerView: ItemChainAccount) : GroupedListHolder(containerView) {

    fun bind(
        item: AccountInChainUi,
        handler: ChainAccountsAdapter.Handler,
        imageLoader: ImageLoader
    ) = with(containerView) {
        chainIcon.loadChainIcon(item.chainUi.icon, imageLoader)
        chainName.text = item.chainUi.name

        accountIcon.setImageDrawable(item.accountIcon)
        accountAddress.text = item.addressOrHint

        action.setVisible(item.actionsAvailable)
        if (item.actionsAvailable) {
            setOnClickListener { handler.chainAccountClicked(item) }
        } else {
            setOnClickListener(null)
        }
    }
}

private object DiffCallback : BaseGroupedDiffCallback<TextHeader, AccountInChainUi>(TextHeader::class.java) {

    override fun areGroupItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
    }

    override fun areGroupContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
    }

    override fun areChildItemsTheSame(oldItem: AccountInChainUi, newItem: AccountInChainUi): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areChildContentsTheSame(oldItem: AccountInChainUi, newItem: AccountInChainUi): Boolean {
        return oldItem.chainUi == newItem.chainUi && oldItem.addressOrHint == newItem.addressOrHint
    }
}
