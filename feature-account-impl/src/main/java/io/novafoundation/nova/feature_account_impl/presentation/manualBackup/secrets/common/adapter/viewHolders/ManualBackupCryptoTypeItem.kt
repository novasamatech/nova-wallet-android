package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import androidx.core.view.isGone
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupCryptoTypeBinding
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder

data class ManualBackupCryptoTypeRvItem(
    val network: String,
    val cryptoTypeTitle: String,
    val cryptoTypeSubtitle: String,
    val derivationPath: String?,
    val hideDerivationPath: Boolean
) : ManualBackupSecretsRvItem {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupCryptoTypeRvItem && network == other.network
    }
}

class ManualBackupCryptoTypeViewHolder(private val binder: ItemManualBackupCryptoTypeBinding) : ManualBackupSecretsViewHolder(binder.root) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupCryptoTypeRvItem)
        binder.manualBackupSecretsCryptoType.setLabel(item.cryptoTypeTitle)
        binder.manualBackupSecretsCryptoType.setMessage(item.cryptoTypeSubtitle)
        binder.manualBackupSecretsDerivationPath.setMessage(item.derivationPath)
        binder.manualBackupSecretsDerivationPathLabel.isGone = item.hideDerivationPath
        binder.manualBackupSecretsDerivationPath.isGone = item.hideDerivationPath
    }
}
