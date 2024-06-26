package io.novafoundation.nova.feature_account_impl.presentation.account.management

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder.Mode
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload.FlowType
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WalletManagmentViewModel(
    private val accountInteractor: AccountInteractor,
    private val accountRouter: AccountRouter,
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
    cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory,
    listSelectorMixinFactory: ListSelectorMixin.Factory,
) : BaseViewModel() {

    val cloudBackupChangingWarningMixin = cloudBackupChangingWarningMixinFactory.create(viewModelScope)

    val walletsListingMixin = accountListingMixinFactory.create(this)

    val mode = MutableStateFlow(Mode.SELECT)

    val listSelectorMixin = listSelectorMixinFactory.create(viewModelScope)

    val toolbarAction = mode.map {
        if (it == Mode.SELECT) {
            resourceManager.getString(R.string.common_edit)
        } else {
            resourceManager.getString(R.string.common_done)
        }
    }
        .shareInBackground()

    val confirmAccountDeletion = actionAwaitableMixinFactory.confirmingOrDenyingAction<Unit>()

    fun accountClicked(accountModel: AccountUi) {
        accountRouter.openWalletDetails(accountModel.id)
    }

    fun editClicked() {
        val newMode = if (mode.value == Mode.SELECT) Mode.EDIT else Mode.SELECT

        mode.value = newMode
    }

    fun deleteClicked(account: AccountUi) {
        cloudBackupChangingWarningMixin.launchRemovingConfirmationIfNeeded {
            launch {
                val deleteConfirmed = confirmAccountDeletion.awaitAction()

                if (deleteConfirmed) {
                    val isAllMetaAccountsWasDeleted = accountInteractor.deleteAccount(account.id)
                    if (isAllMetaAccountsWasDeleted) {
                        accountRouter.openWelcomeScreen()
                    }
                }
            }
        }
    }

    fun backClicked() {
        accountRouter.back()
    }

    fun addAccountClicked() {
        listSelectorMixin.showSelector(
            R.string.wallet_management_add_account_title,
            listOf(createWalletItem(), importWalletItem())
        )
    }

    private fun createWalletItem(): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_add_circle_outline,
            R.color.icon_primary,
            R.string.account_create_wallet,
            R.color.text_primary,
            ::onCreateNewWalletClicked
        )
    }

    private fun importWalletItem(): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_import,
            R.color.icon_primary,
            R.string.account_export_existing,
            R.color.text_primary,
            ::onImportWalletClicked
        )
    }

    private fun onImportWalletClicked() {
        accountRouter.openImportOptionsScreen()
    }

    private fun onCreateNewWalletClicked() {
        cloudBackupChangingWarningMixin.launchChangingConfirmationIfNeeded {
            accountRouter.openCreateWallet(StartCreateWalletPayload(FlowType.SECOND_WALLET))
        }
    }
}
