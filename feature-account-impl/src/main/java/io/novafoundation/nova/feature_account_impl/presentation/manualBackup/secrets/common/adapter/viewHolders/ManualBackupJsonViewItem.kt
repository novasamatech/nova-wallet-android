package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupJsonBinding
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder

class ManualBackupJsonRvItem : ManualBackupSecretsRvItem {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupJsonRvItem
    }

    override fun equals(other: Any?): Boolean {
        return other is ManualBackupJsonRvItem
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

class ManualBackupJsonViewHolder(private val binder: ItemManualBackupJsonBinding, private val itemHandler: ManualBackupItemHandler) :
    ManualBackupSecretsViewHolder(binder.root) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupJsonRvItem)
        binder.manualBackupSecretsJsonButton.setOnClickListener { itemHandler.onExportJsonClick(item) }
    }
}
