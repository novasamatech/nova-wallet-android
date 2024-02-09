package io.novafoundation.nova.feature_account_api.presenatation.account.listing

import android.view.ViewGroup
import androidx.annotation.ColorRes
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi

fun interface AccountGroupViewHolderFactory {
    fun create(parent: ViewGroup): GroupedListHolder
}

fun interface AccountGroupViewHolderBinder<Group : AccountGroupRvItem> {
    fun bind(holder: GroupedListHolder, item: Group)
}

interface AccountGroupRvItem {
    fun isItemTheSame(other: AccountGroupRvItem): Boolean
}

abstract class CommonAccountsAdapter<Group : AccountGroupRvItem>(
    private val accountItemHandler: AccountHolder.AccountItemHandler?,
    private val imageLoader: ImageLoader,
    private val diffCallback: AccountDiffCallback<Group>,
    private val groupFactory: AccountGroupViewHolderFactory,
    private val groupBinder: AccountGroupViewHolderBinder<Group>,
    @ColorRes private val chainBorderColor: Int,
    initialMode: AccountHolder.Mode,
) : GroupedListAdapter<Group, AccountUi>(diffCallback) {

    private var mode: AccountHolder.Mode = initialMode

    fun setMode(mode: AccountHolder.Mode) {
        this.mode = mode

        notifyItemRangeChanged(0, itemCount, mode)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return groupFactory.create(parent)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return AccountHolder(parent.inflateChild(R.layout.item_account), imageLoader, chainBorderColor)
    }

    override fun bindGroup(holder: GroupedListHolder, group: Group) {
        groupBinder.bind(holder, group)
    }

    override fun bindChild(holder: GroupedListHolder, child: AccountUi) {
        (holder as AccountHolder).bind(mode, child, accountItemHandler)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: AccountUi, payloads: List<Any>) {
        require(holder is AccountHolder)

        resolvePayload(
            holder,
            position,
            payloads,
            onUnknownPayload = { holder.bindMode(mode, child, accountItemHandler) },
            onDiffCheck = {
                when (it) {
                    AccountUi::title -> holder.bindName(child)
                    AccountUi::subtitle -> holder.bindSubtitle(child)
                    AccountUi::isSelected -> holder.bindMode(mode, child, accountItemHandler)
                }
            }
        )
    }
}

class AccountDiffCallback<Group : AccountGroupRvItem>(groupClass: Class<Group>) : BaseGroupedDiffCallback<Group, AccountUi>(groupClass) {
    override fun areGroupItemsTheSame(oldItem: Group, newItem: Group): Boolean {
        return oldItem.isItemTheSame(newItem)
    }

    override fun areGroupContentsTheSame(oldItem: Group, newItem: Group): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: AccountUi, newItem: AccountUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: AccountUi, newItem: AccountUi): Boolean {
        return oldItem.title == newItem.title && oldItem.subtitle == newItem.subtitle && oldItem.isSelected == newItem.isSelected
    }

    override fun getChildChangePayload(oldItem: AccountUi, newItem: AccountUi): Any? {
        return MetaAccountPayloadGenerator.diff(oldItem, newItem)
    }
}
