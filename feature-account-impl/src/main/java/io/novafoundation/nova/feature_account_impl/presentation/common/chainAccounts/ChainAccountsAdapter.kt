package io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.headers.TextHeaderHolder
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.item_chain_acount.view.chainAccountAccountAddress
import kotlinx.android.synthetic.main.item_chain_acount.view.chainAccountAccountIcon
import kotlinx.android.synthetic.main.item_chain_acount.view.chainAccountChainIcon
import kotlinx.android.synthetic.main.item_chain_acount.view.chainAccountChainName
import kotlinx.android.synthetic.main.item_chain_acount.view.labeledTextAction

class ChainAccountsAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : GroupedListAdapter<TextHeader, AccountInChainUi>(DiffCallback) {

    interface Handler {

        fun chainAccountClicked(item: AccountInChainUi)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return TextHeaderHolder(parent)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ChainAccountHolder(parent.inflateChild(R.layout.item_chain_acount))
    }

    override fun bindGroup(holder: GroupedListHolder, group: TextHeader) {
        holder.castOrNull<TextHeaderHolder>()?.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: AccountInChainUi) {
        holder.castOrNull<ChainAccountHolder>()?.bind(child, handler, imageLoader)
    }
}

class ChainAccountHolder(view: View) : GroupedListHolder(view) {

    fun bind(
        item: AccountInChainUi,
        handler: ChainAccountsAdapter.Handler,
        imageLoader: ImageLoader
    ) = with(containerView) {
        chainAccountChainIcon.loadChainIcon(item.chainUi.icon, imageLoader)
        chainAccountChainName.text = item.chainUi.name

        chainAccountAccountIcon.setImageDrawable(item.accountIcon)
        chainAccountAccountAddress.text = item.addressOrHint

        labeledTextAction.setVisible(item.actionsAvailable)
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
