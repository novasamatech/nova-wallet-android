package io.novafoundation.nova.app.root.navigation.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerPayload

class LedgerNavigator(
    private val accountRouter: AccountRouter,
    navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), LedgerRouter {

    override fun openImportFillWallet() = performNavigation(R.id.action_startImportLedgerFragment_to_fillWalletImportLedgerFragment)

    override fun returnToImportFillWallet() = performNavigation(R.id.action_selectAddressImportLedgerFragment_to_fillWalletImportLedgerFragment)

    override fun openSelectImportAddress(payload: SelectLedgerAddressPayload) = performNavigation(
        actionId = R.id.action_selectLedgerImportFragment_to_selectAddressImportLedgerFragment,
        args = SelectAddressLedgerFragment.getBundle(payload)
    )

    override fun openCreatePincode() {
        accountRouter.openCreatePincode()
    }

    override fun openMain() {
        accountRouter.openMain()
    }

    override fun openFinishImportLedger(payload: FinishImportLedgerPayload) = performNavigation(
        actionId = R.id.action_fillWalletImportLedgerFragment_to_finishImportLedgerFragment,
        args = FinishImportLedgerFragment.getBundle(payload)
    )

    override fun finishSignFlow() {
        back()
    }

    override fun openAddChainAccountSelectAddress(payload: AddLedgerChainAccountSelectAddressPayload) = performNavigation(
        actionId = R.id.action_addChainAccountSelectLedgerFragment_to_addChainAccountSelectAddressLedgerFragment,
        args = AddLedgerChainAccountSelectAddressFragment.getBundle(payload)
    )

    override fun openSelectLedgerGeneric() = performNavigation(R.id.action_startImportGenericLedgerFragment_to_selectLedgerGenericImportFragment)

    override fun openPreviewLedgerAccountsGeneric(payload: PreviewImportGenericLedgerPayload) = performNavigation(
        actionId = R.id.action_selectLedgerGenericImportFragment_to_previewImportGenericLedgerFragment,
        args = PreviewImportGenericLedgerFragment.getBundle(payload)
    )

    override fun openFinishImportLedgerGeneric(payload: FinishImportGenericLedgerPayload) = performNavigation(
        actionId = R.id.action_previewImportGenericLedgerFragment_to_finishImportGenericLedgerFragment,
        args = FinishImportGenericLedgerFragment.getBundle(payload)
    )
}
