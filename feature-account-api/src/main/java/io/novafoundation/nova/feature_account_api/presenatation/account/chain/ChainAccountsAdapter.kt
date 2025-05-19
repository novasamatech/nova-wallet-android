package io.novafoundation.nova.feature_account_api.presenatation.account.chain

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import coil.ImageLoader
import io.novafoundation.nova.common.databinding.ItemTextHeaderBinding
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.headers.TextHeaderHolder
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ItemChainAccountGroupBinding
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.ChainAccountGroupUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_api.view.ItemChainAccount

class ChainAccountsAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : GroupedListAdapter<ChainAccountGroupUi, AccountInChainUi>(DiffCallback()) {

    interface Handler {

        fun chainAccountClicked(item: AccountInChainUi)

        fun onGroupActionClicked(item: ChainAccountGroupUi) {}
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ChainAccountGroupHolder(
            viewBinding = ItemChainAccountGroupBinding.inflate(parent.inflater(), parent, false),
            handler = handler
        )
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = ItemChainAccount(parent.context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        return ChainAccountHolder(view)
    }

    override fun bindGroup(holder: GroupedListHolder, group: ChainAccountGroupUi) {
        holder.castOrNull<ChainAccountGroupHolder>()?.bind(group)
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

class ChainAccountGroupHolder(
    private val viewBinding: ItemChainAccountGroupBinding,
    private val handler: ChainAccountsAdapter.Handler,
): GroupedListHolder(viewBinding.root) {

    fun bind(item: ChainAccountGroupUi) = with(viewBinding) {
        itemChainAccountGroupTitle.text = item.title

        val action = item.action
        if (action != null) {
            itemChainAccountGroupAction.makeVisible()

            itemChainAccountGroupAction.text = action.name
            itemChainAccountGroupAction.setDrawableStart(action.icon, widthInDp = 16, paddingInDp = 4, tint = R.color.icon_accent)

            itemChainAccountGroupAction.setOnClickListener { handler.onGroupActionClicked(item) }
        } else {
            itemChainAccountGroupAction.makeGone()
            itemChainAccountGroupAction.setOnClickListener(null)
        }
    }
}

private class DiffCallback : BaseGroupedDiffCallback<ChainAccountGroupUi, AccountInChainUi>(ChainAccountGroupUi::class.java) {

    override fun areGroupItemsTheSame(oldItem: ChainAccountGroupUi, newItem: ChainAccountGroupUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areGroupContentsTheSame(oldItem: ChainAccountGroupUi, newItem: ChainAccountGroupUi): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: AccountInChainUi, newItem: AccountInChainUi): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areChildContentsTheSame(oldItem: AccountInChainUi, newItem: AccountInChainUi): Boolean {
        return oldItem.chainUi == newItem.chainUi && oldItem.addressOrHint == newItem.addressOrHint
    }
}
