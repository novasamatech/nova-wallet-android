package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.wallets

import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.SIZE_BIG
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.ManualBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import kotlinx.coroutines.launch

class ManualBackupSelectWalletViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    private val manualBackupInteractor: ManualBackupInteractor,
    private val walletUiUseCase: WalletUiUseCase
) : BaseViewModel() {

    private val wallets = flowOf { manualBackupInteractor.getBackupableMetaAccounts() }
        .shareInBackground()

    val walletsUI = wallets.mapList { mapMetaAccountToUI(it) }

    fun walletClicked(accountModel: AccountUi) {
        launch {
            val metaAccount = manualBackupInteractor.getMetaAccount(accountModel.id)
            if (metaAccount.chainAccounts.isEmpty()) {
                router.openManualBackupConditions(ManualBackupCommonPayload.DefaultAccount(metaId = accountModel.id))
            } else {
                router.openManualBackupSelectAccount(accountModel.id)
            }
        }
    }

    private suspend fun mapMetaAccountToUI(metaAccount: MetaAccount): AccountUi {
        return AccountUi(
            id = metaAccount.id,
            title = metaAccount.name,
            subtitle = null,
            isSelected = false,
            isClickable = true,
            picture = walletUiUseCase.walletIcon(metaAccount, iconSize = SIZE_BIG),
            chainIconUrl = null,
            updateIndicator = false,
            subtitleIconRes = null,
            chainIconOpacity = 1f,
            isEditable = false
        )
    }

    fun backClicked() {
        router.back()
    }
}
