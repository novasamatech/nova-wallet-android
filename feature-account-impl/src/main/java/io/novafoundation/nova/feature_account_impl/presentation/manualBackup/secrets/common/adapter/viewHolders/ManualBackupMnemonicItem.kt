package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupMnemonicBinding
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

data class ManualBackupMnemonicRvItem(
    val mnemonic: List<String>,
    override var isShown: Boolean
) : ManualBackupSecretsVisibilityRvItem() {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupMnemonicRvItem
    }
}

class ManualBackupMnemonicViewHolder(private val binder: ItemManualBackupMnemonicBinding, private val itemHandler: ManualBackupItemHandler) :
    ManualBackupSecretsViewHolder(binder.root) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupMnemonicRvItem)

        binder.manualBackupSecretsMnemonic.setWordsString(item.mnemonic)
        binder.manualBackupSecretsMnemonic.showContent(item.isShown)
        binder.manualBackupSecretsMnemonic.onContentShownListener { itemHandler.onTapToRevealClicked(item) }
    }
}
