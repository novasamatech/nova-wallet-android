package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import android.view.View
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

class ManualBackupJsonViewHolder(itemView: View, private val itemHandler: ManualBackupItemHandler) : ManualBackupSecretsViewHolder(itemView) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupJsonRvItem)
        itemView.manualBackupSecretsJsonButton.setOnClickListener { itemHandler.onExportJsonClick(item) }
    }
}
