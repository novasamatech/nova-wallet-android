package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.walletUiFlowFor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.export.CommonExportSecretsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.ManualBackupSecretsAdapterItemFactory
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.getChainIdOrNull
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem

class ManualBackupSecretsViewModel(
    private val resourceManager: ResourceManager,
    private val router: AccountRouter,
    private val payload: ManualBackupCommonPayload,
    private val commonExportSecretsInteractor: CommonExportSecretsInteractor,
    private val secretsAdapterItemFactory: ManualBackupSecretsAdapterItemFactory,
    private val walletUiUseCase: WalletUiUseCase
) : BaseViewModel() {

    val walletModel = walletUiUseCase.walletUiFlowFor(payload.metaId, payload.getChainIdOrNull(), showAddressIcon = true)

    val advancedSecretsBtnAvailable = flowOf { commonExportSecretsInteractor.hasMnemonic(payload.metaId, payload.getChainIdOrNull()) }
        .shareInBackground()

    val exportList = flowOf { buildSecrets() }
        .shareInBackground()

    fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem) {
        // It's not necessary to update the list, because the item will play a show animation. We just need to update its state
        item.makeShown()
    }

    fun advancedSecretsClicked() {
        router.openManualBackupAdvancedSecrets(payload)
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun buildSecrets(): List<ManualBackupSecretsRvItem> = buildList {
        if (payload is ManualBackupCommonPayload.ChainAccount) {
            this += secretsAdapterItemFactory.createChainItem(payload.chainId)
            this += secretsAdapterItemFactory.createTitle(resourceManager.getString(R.string.manual_backup_secrets_custom_key_title))
        } else {
            this += secretsAdapterItemFactory.createTitle(resourceManager.getString(R.string.manual_backup_secrets_default_key_title))
        }

        val mnemonicItem = secretsAdapterItemFactory.createMnemonic(payload.metaId, payload.getChainIdOrNull())

        if (mnemonicItem == null) {
            this += secretsAdapterItemFactory.createSubtitle(resourceManager.getString(R.string.manual_backup_secrets_subtitle))
            this += secretsAdapterItemFactory.createAdvancedSecrets(payload.metaId, payload.getChainIdOrNull())
        } else {
            this += mnemonicItem
        }
    }
}
