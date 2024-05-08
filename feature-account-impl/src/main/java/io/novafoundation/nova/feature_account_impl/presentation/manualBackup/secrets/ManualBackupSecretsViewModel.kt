package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.walletUiFlowFor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.getChainIdOrNull
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsVisibilityRvItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ManualBackupSecretsViewModel(
    private val resourceManager: ResourceManager,
    private val router: AccountRouter,
    private val payload: ManualBackupCommonPayload,
    private val secretsAdapterItemFactory: ManualBackupSecretsAdapterItemFactory,
    private val walletUiUseCase: WalletUiUseCase
) : BaseViewModel() {

    val walletModel = walletUiUseCase.walletUiFlowFor(payload.metaId, payload.getChainIdOrNull(), showAddressIcon = false)

    val exportList = MutableStateFlow<List<ManualBackupSecretsRvItem>>(listOf())

    init {
        initExportList()
    }

    private fun initExportList() {
        launch {
            exportList.value = buildList {
                if (payload is ManualBackupCommonPayload.ChainAccount) {
                    add(secretsAdapterItemFactory.createChainItem(payload.chainId))
                    add(secretsAdapterItemFactory.createTitle(resourceManager.getString(R.string.manual_backup_secrets_custom_key_title)))
                } else {
                    add(secretsAdapterItemFactory.createTitle(resourceManager.getString(R.string.manual_backup_secrets_default_key_title)))
                }

                val mnemonicItem = secretsAdapterItemFactory.createMnemonic(payload.metaId, payload.getChainIdOrNull())

                if (mnemonicItem == null) {
                    val subtitle = secretsAdapterItemFactory.createSubtitle(resourceManager.getString(R.string.manual_backup_secrets_subtitle))
                    val advancedSecrets = secretsAdapterItemFactory.createAdvancedSecrets(payload.metaId, payload.getChainIdOrNull())

                    add(subtitle)
                    addAll(advancedSecrets)
                } else {
                    add(mnemonicItem)
                }
            }
        }
    }

    fun onTapToRevealClicked(item: ManualBackupSecretsVisibilityRvItem) {
        // It's not necessary to update the list, because the item will play a show animation. We just need to update its state
        item.makeShown()
    }

    fun backClicked() {
        router.back()
    }
}
