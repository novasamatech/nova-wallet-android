package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsViewHolder
import kotlinx.android.synthetic.main.item_manual_backup_chain.view.manualBackupSecretsChain

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
