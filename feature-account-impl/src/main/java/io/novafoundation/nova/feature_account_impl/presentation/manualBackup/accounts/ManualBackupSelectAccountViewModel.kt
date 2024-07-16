package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.ManualBackupSelectAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.MetaAccountChains
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupAccountGroupRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupAccountRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter.ManualBackupRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import kotlinx.coroutines.flow.map

class ManualBackupSelectAccountViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    private val manualBackupSelectAccountInteractor: ManualBackupSelectAccountInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val payload: ManualBackupSelectAccountPayload
) : BaseViewModel() {

    private val metaAccountChains = manualBackupSelectAccountInteractor.sortedMetaAccountChains(payload.metaId)
        .shareInBackground()

    val walletModel = metaAccountChains.map { metaAccountChains ->
        walletUiUseCase.walletUiFor(metaAccountChains.metaAccount)
    }

    val accountsList = metaAccountChains.map { metaAccountChains ->
        mapMetaAccountToUI(metaAccountChains)
    }

    fun walletClicked(accountModel: ManualBackupAccountRvItem) {
        val payload = if (accountModel.chainId == null) {
            ManualBackupCommonPayload.DefaultAccount(metaId = this.payload.metaId)
        } else {
            ManualBackupCommonPayload.ChainAccount(metaId = this.payload.metaId, chainId = accountModel.chainId)
        }

        router.openManualBackupConditions(payload)
    }

    fun backClicked() {
        router.back()
    }

    private fun mapMetaAccountToUI(
        metaAccountChains: MetaAccountChains
    ): List<ManualBackupRvItem> {
        return buildList {
            if (metaAccountChains.defaultChains.isNotEmpty()) {
                this += ManualBackupAccountGroupRvItem(resourceManager.getString(R.string.manual_backup_select_account_default_key_title))

                val firstChains = metaAccountChains.defaultChains.take(2)
                    .joinToString { it.name }
                val remainingChains = metaAccountChains.defaultChains.drop(2)
                val subtitle = if (remainingChains.isNotEmpty()) {
                    resourceManager.getString(R.string.manual_backup_select_account_default_key_account_subtitle_more_chains, firstChains, remainingChains.size)
                } else {
                    firstChains
                }

                this += ManualBackupAccountRvItem(
                    chainId = null, // It's null for default account
                    icon = resourceManager.getDrawable(R.drawable.ic_nova_logo).asIcon(),
                    title = resourceManager.getString(R.string.manual_backup_select_account_default_key_account),
                    subtitle = subtitle
                )
            }

            if (metaAccountChains.customChains.isNotEmpty()) {
                this += ManualBackupAccountGroupRvItem(resourceManager.getString(R.string.manual_backup_select_account_custom_key_title))

                metaAccountChains.customChains.forEach { chain ->
                    this += ManualBackupAccountRvItem(
                        chainId = chain.id,
                        icon = chain.iconOrFallback(),
                        title = chain.name,
                        subtitle = null
                    )
                }
            }
        }
    }
}
