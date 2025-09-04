package io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.actionAwaitable.fromRes
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsResponder
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.SelectedMetaAccountState
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectMultipleWalletsViewModel(
    private val router: AccountRouter,
    private val request: SelectMultipleWalletsRequester.Request,
    private val responder: SelectMultipleWalletsResponder,
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
) : WalletListViewModel() {

    val closeConfirmationAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val titleFlow = flowOf(request.titleText)

    val selectedMetaAccounts = MutableStateFlow(SelectedMetaAccountState.Specified(request.currentlySelectedMetaIds))

    override val walletsListingMixin = accountListingMixinFactory.create(
        coroutineScope = this,
        showUpdatedMetaAccountsBadge = false,
        metaAccountSelectedFlow = selectedMetaAccounts
    )

    override val mode: AccountHolder.Mode = AccountHolder.Mode.SELECT_MULTIPLE

    val confirmButtonState = selectedMetaAccounts.map { selectedMetaAccounts ->
        if (selectedMetaAccounts.ids.size < request.min) {
            val disabledText = resourceManager.getQuantityString(R.plurals.multiple_wallets_selection_min_button_text, request.min, request.min)
            DescriptiveButtonState.Disabled(disabledText)
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
    }

    fun backClicked() {
        launch {
            val dataHasBeenChanged = selectedMetaAccounts.value.ids != request.currentlySelectedMetaIds

            if (dataHasBeenChanged) {
                closeConfirmationAction.awaitAction(
                    ConfirmationDialogInfo.fromRes(
                        resourceManager,
                        R.string.common_confirmation_title,
                        R.string.common_close_confirmation_message,
                        R.string.common_close,
                        R.string.common_cancel,
                    )
                )
            }

            router.back()
        }
    }

    fun confirm() {
        responder.respond(SelectMultipleWalletsResponder.Response(selectedMetaAccounts.value.ids))
        router.back()
    }

    override fun accountClicked(accountModel: AccountUi) {
        val selected = mutableSetOf(*selectedMetaAccounts.value.ids.toTypedArray())

        if (selected.contains(accountModel.id)) {
            selected.remove(accountModel.id)
        } else {
            if (selected.size >= request.max) {
                showToast(resourceManager.getString(R.string.multiple_wallets_selection_max_message, request.max))
                return
            }

            selected.add(accountModel.id)
        }

        selectedMetaAccounts.value = SelectedMetaAccountState.Specified(selected)
    }
}
