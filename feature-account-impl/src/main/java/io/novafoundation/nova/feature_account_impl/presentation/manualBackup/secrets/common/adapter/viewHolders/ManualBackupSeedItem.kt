package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import android.view.View
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.ManualBackupItemHandler
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

data class ManualBackupSeedRvItem(
    val label: String,
    val seed: String,
    override var isShown: Boolean
) : ManualBackupSecretsVisibilityRvItem() {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupSeedRvItem
    }
}

class ManualBackupSeedViewHolder(itemView: View, private val itemHandler: ManualBackupItemHandler) : ManualBackupSecretsViewHolder(itemView) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupSeedRvItem)
        itemView.manualBackupSecretsSeedLabel.text = item.label
        itemView.manualBackupSecretsSeedContainer.showContent(item.isShown)
        itemView.manualBackupSecretsSeedText.text = item.seed

        itemView.manualBackupSecretsSeedContainer.onContentShownListener { itemHandler.onTapToRevealClicked(item) }
    }
}
