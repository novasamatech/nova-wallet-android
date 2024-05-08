package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter

import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

interface ManualBackupItemHandler {

    fun onExportJsonClick(item: ManualBackupJsonRvItem)

    fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem)
}
