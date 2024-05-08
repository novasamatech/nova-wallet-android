package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders

import android.view.View
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem
import kotlinx.android.synthetic.main.item_manual_backup_mnemonic.view.manualBackupSecretsMnemonic

data class ManualBackupMnemonicRvItem(
    val mnemonic: List<String>,
    override var isShown: Boolean
) : ManualBackupSecretsVisibilityRvItem() {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupMnemonicRvItem
    }
}

class ManualBackupMnemonicViewHolder(itemView: View, private val itemHandler: ManualBackupItemHandler) : ManualBackupSecretsViewHolder(itemView) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupMnemonicRvItem)

        itemView.manualBackupSecretsMnemonic.setWordsString(item.mnemonic)
        itemView.manualBackupSecretsMnemonic.showContent(item.isShown)
        itemView.manualBackupSecretsMnemonic.onContentShownListener { itemHandler.onTapToRevealClicked(item) }
    }
}
