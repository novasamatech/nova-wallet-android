package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupSeedBinding
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

class ManualBackupSeedViewHolder(private val binder: ItemManualBackupSeedBinding, private val itemHandler: ManualBackupItemHandler) :
    ManualBackupSecretsViewHolder(binder.root) {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupSeedRvItem)
        binder.manualBackupSecretsSeedLabel.text = item.label
        binder.manualBackupSecretsSeedContainer.showContent(item.isShown)
        binder.manualBackupSecretsSeedText.text = item.seed

        binder.manualBackupSecretsSeedContainer.onContentShownListener { itemHandler.onTapToRevealClicked(item) }
    }
}
