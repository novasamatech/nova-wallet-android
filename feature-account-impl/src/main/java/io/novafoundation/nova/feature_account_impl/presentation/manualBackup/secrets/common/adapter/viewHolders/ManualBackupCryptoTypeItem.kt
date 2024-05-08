package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import android.view.View
import androidx.core.view.isGone
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder
import kotlinx.android.synthetic.main.item_manual_backup_crypto_type.view.manualBackupSecretsCryptoType
import kotlinx.android.synthetic.main.item_manual_backup_crypto_type.view.manualBackupSecretsDerivationPath
import kotlinx.android.synthetic.main.item_manual_backup_crypto_type.view.manualBackupSecretsDerivationPathLabel


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

class ManualBackupCryptoTypeViewHolder(itemView: View) : ManualBackupSecretsViewHolder(itemView) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupCryptoTypeRvItem)
        itemView.manualBackupSecretsCryptoType.setLabel(item.cryptoTypeTitle)
        itemView.manualBackupSecretsCryptoType.setMessage(item.cryptoTypeSubtitle)
        itemView.manualBackupSecretsDerivationPath.setMessage(item.derivationPath)
        itemView.manualBackupSecretsDerivationPathLabel.isGone = item.hideDerivationPath
        itemView.manualBackupSecretsDerivationPath.isGone = item.hideDerivationPath
    }
}
