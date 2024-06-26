package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.item_backup_account.view.itemManualBackupAccountContainer
import kotlinx.android.synthetic.main.item_backup_account.view.itemManualBackupAccountIcon
import kotlinx.android.synthetic.main.item_backup_account.view.itemManualBackupAccountSubtitle
import kotlinx.android.synthetic.main.item_backup_account.view.itemManualBackupAccountTitle
import kotlinx.android.synthetic.main.item_backup_account_header.view.itemManualBackupGroupTitle

class ManualBackupAccountsAdapter(
    private val imageLoader: ImageLoader,
    private val accountHandler: AccountHandler
) : GroupedListAdapter<ManualBackupAccountGroupRvItem, ManualBackupAccountRvItem>(AccountDiffCallback()) {

    interface AccountHandler {
        fun onAccountClicked(account: ManualBackupAccountRvItem)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ManualBackupGroupViewHolder(parent.inflateChild(R.layout.item_backup_account_header))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ManualBackupAccountViewHolder(parent.inflateChild(R.layout.item_backup_account), imageLoader, accountHandler)
    }

    override fun bindGroup(holder: GroupedListHolder, group: ManualBackupAccountGroupRvItem) {
        (holder as ManualBackupGroupViewHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: ManualBackupAccountRvItem) {
        (holder as ManualBackupAccountViewHolder).bind(child)
    }
}

class ManualBackupGroupViewHolder(view: View) : GroupedListHolder(view) {

    fun bind(item: ManualBackupAccountGroupRvItem) {
        itemView.itemManualBackupGroupTitle.text = item.text
    }
}

class ManualBackupAccountViewHolder(
    view: View,
    private val imageLoader: ImageLoader,
    private val accountHandler: ManualBackupAccountsAdapter.AccountHandler
) : GroupedListHolder(view) {

    fun bind(item: ManualBackupAccountRvItem) {
        with(itemView) {
            itemManualBackupAccountContainer.background = context.addRipple(context.getBlockDrawable())
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
