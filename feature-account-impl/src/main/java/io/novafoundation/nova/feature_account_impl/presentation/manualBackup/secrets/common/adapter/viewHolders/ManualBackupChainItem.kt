package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import android.view.View
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder

data class ManualBackupChainRvItem(
    val chainModel: ChainUi
) : ManualBackupSecretsRvItem {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupChainRvItem
    }
}

class ManualBackupChainViewHolder(itemView: View) : ManualBackupSecretsViewHolder(itemView) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupChainRvItem)
        itemView.manualBackupSecretsChain.setChain(item.chainModel)
    }
}
