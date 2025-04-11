package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter

import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_impl.databinding.ItemBackupAccountBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemBackupAccountHeaderBinding

class ManualBackupAccountsAdapter(
    private val imageLoader: ImageLoader,
    private val accountHandler: AccountHandler
) : GroupedListAdapter<ManualBackupAccountGroupRvItem, ManualBackupAccountRvItem>(AccountDiffCallback()) {

    interface AccountHandler {
        fun onAccountClicked(account: ManualBackupAccountRvItem)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ManualBackupGroupViewHolder(ItemBackupAccountHeaderBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ManualBackupAccountViewHolder(ItemBackupAccountBinding.inflate(parent.inflater(), parent, false), imageLoader, accountHandler)
    }

    override fun bindGroup(holder: GroupedListHolder, group: ManualBackupAccountGroupRvItem) {
        (holder as ManualBackupGroupViewHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: ManualBackupAccountRvItem) {
        (holder as ManualBackupAccountViewHolder).bind(child)
    }
}

class ManualBackupGroupViewHolder(private val binder: ItemBackupAccountHeaderBinding) : GroupedListHolder(binder.root) {

    fun bind(item: ManualBackupAccountGroupRvItem) {
        binder.itemManualBackupGroupTitle.text = item.text
    }
}

class ManualBackupAccountViewHolder(
    private val binder: ItemBackupAccountBinding,
    private val imageLoader: ImageLoader,
    private val accountHandler: ManualBackupAccountsAdapter.AccountHandler
) : GroupedListHolder(binder.root) {

    fun bind(item: ManualBackupAccountRvItem) {
        with(binder) {
            itemManualBackupAccountContainer.background = binder.root.context.addRipple(binder.root.context.getBlockDrawable())
            itemManualBackupAccountIcon.setIcon(item.icon, imageLoader)
            itemManualBackupAccountTitle.text = item.title
            itemManualBackupAccountSubtitle.setTextOrHide(item.subtitle)
            itemManualBackupAccountContainer.setOnClickListener { accountHandler.onAccountClicked(item) }
        }
    }
}

class AccountDiffCallback : BaseGroupedDiffCallback<ManualBackupAccountGroupRvItem, ManualBackupAccountRvItem>(ManualBackupAccountGroupRvItem::class.java) {

    override fun areGroupItemsTheSame(oldItem: ManualBackupAccountGroupRvItem, newItem: ManualBackupAccountGroupRvItem): Boolean {
        return oldItem.text == newItem.text
    }

    override fun areGroupContentsTheSame(oldItem: ManualBackupAccountGroupRvItem, newItem: ManualBackupAccountGroupRvItem): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: ManualBackupAccountRvItem, newItem: ManualBackupAccountRvItem): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areChildContentsTheSame(oldItem: ManualBackupAccountRvItem, newItem: ManualBackupAccountRvItem): Boolean {
        return true
    }
}
