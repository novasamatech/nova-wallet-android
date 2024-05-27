package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.getChainIdOrNull
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.toExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.ManualBackupSecretsAdapterItemFactory
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

class ManualBackupAdvancedSecretsViewModel(
    private val resourceManager: ResourceManager,
    private val router: AccountRouter,
    private val payload: ManualBackupCommonPayload,
    private val secretsAdapterItemFactory: ManualBackupSecretsAdapterItemFactory
) : BaseViewModel() {

    val exportList = flowOf { buildSecrets() }
        .shareInBackground()

    fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem) {
        // It's not necessary to update the list, because the item will play a show animation. We just need to update its state
        item.makeShown()
    }

    fun exportJsonClicked() {
        router.exportJsonAction(payload.toExportPayload())
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun buildSecrets(): List<ManualBackupSecretsRvItem> = buildList {
        this += secretsAdapterItemFactory.createSubtitle(resourceManager.getString(R.string.manual_backup_secrets_subtitle))
        this += secretsAdapterItemFactory.createAdvancedSecrets(payload.metaId, payload.getChainIdOrNull())
    }
}
