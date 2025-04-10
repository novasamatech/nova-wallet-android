package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.adapter

import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.ChipLabelView
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountChipHolder
import io.novafoundation.nova.feature_settings_impl.databinding.ItemCloudBackupAccountDiffBinding

class CloudBackupDiffAdapter : GroupedListAdapter<CloudBackupDiffGroupRVItem, AccountDiffRVItem>(BackupAccountDiffCallback()) {

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return AccountChipHolder(ChipLabelView(parent.context))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return AccountDiffHolder(ItemCloudBackupAccountDiffBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: CloudBackupDiffGroupRVItem) {
        (holder as AccountChipHolder).bind(group.chipModel)
    }

    override fun bindChild(holder: GroupedListHolder, child: AccountDiffRVItem) {
        (holder as AccountDiffHolder).bind(child)
    }
}

class AccountDiffHolder(private val binder: ItemCloudBackupAccountDiffBinding) : GroupedListHolder(binder.root) {

    fun bind(accountModel: AccountDiffRVItem) = with(binder) {
        itemCloudBackupAccountDiffIcon.setImageDrawable(accountModel.icon)
        itemCloudBackupAccountDiffName.text = accountModel.title
        itemCloudBackupAccountDiffState.text = accountModel.state
        itemCloudBackupAccountDiffState.setTextColor(root.context.getColor(accountModel.stateColorRes))
        itemCloudBackupAccountDiffState.setDrawableStart(accountModel.stateIconRes, paddingInDp = 4)
    }
}

private class BackupAccountDiffCallback : BaseGroupedDiffCallback<CloudBackupDiffGroupRVItem, AccountDiffRVItem>(CloudBackupDiffGroupRVItem::class.java) {

    override fun areGroupItemsTheSame(oldItem: CloudBackupDiffGroupRVItem, newItem: CloudBackupDiffGroupRVItem): Boolean {
        return oldItem.chipModel.title == newItem.chipModel.title
    }

    override fun areGroupContentsTheSame(oldItem: CloudBackupDiffGroupRVItem, newItem: CloudBackupDiffGroupRVItem): Boolean {
        return oldItem.chipModel.title == newItem.chipModel.title
    }

    override fun areChildItemsTheSame(oldItem: AccountDiffRVItem, newItem: AccountDiffRVItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: AccountDiffRVItem, newItem: AccountDiffRVItem): Boolean {
        return oldItem.id == newItem.id
    }
}
