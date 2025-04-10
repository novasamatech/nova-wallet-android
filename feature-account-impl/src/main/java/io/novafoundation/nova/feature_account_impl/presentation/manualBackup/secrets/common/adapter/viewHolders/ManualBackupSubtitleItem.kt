package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupSubtitleBinding
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder

data class ManualBackupSubtitleRvItem(
    val text: String
) : ManualBackupSecretsRvItem {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupSubtitleRvItem && text == other.text
    }
}

class ManualBackupSubtitleViewHolder(private val binder: ItemManualBackupSubtitleBinding) : ManualBackupSecretsViewHolder(binder.root) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupSubtitleRvItem)
        binder.manualBackupSecretsSubtitle.text = item.text
    }
}
