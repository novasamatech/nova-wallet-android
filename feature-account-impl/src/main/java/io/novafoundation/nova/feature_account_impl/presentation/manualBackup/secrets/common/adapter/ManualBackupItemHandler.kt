package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter

import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

interface ManualBackupItemHandler {

    fun onExportJsonClick(item: ManualBackupJsonRvItem)

    fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem)
}
