package io.novafoundation.nova.app.root.navigation.navigators.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
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
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), LedgerRouter {

    override fun openImportFillWallet() {
        navigationBuilder(R.id.action_startImportLedgerFragment_to_fillWalletImportLedgerFragment)
            .navigateInFirstAttachedContext()
    }

    override fun returnToImportFillWallet() {
        navigationBuilder(R.id.action_selectAddressImportLedgerFragment_to_fillWalletImportLedgerFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectImportAddress(payload: SelectLedgerAddressPayload) {
        navigationBuilder(R.id.action_selectLedgerImportFragment_to_selectAddressImportLedgerFragment)
            .setArgs(SelectAddressLedgerFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openCreatePincode() {
        accountRouter.openCreatePincode()
    }

    override fun openMain() {
        accountRouter.openMain()
    }

    override fun openFinishImportLedger(payload: FinishImportLedgerPayload) {
        navigationBuilder(R.id.action_fillWalletImportLedgerFragment_to_finishImportLedgerFragment)
            .setArgs(FinishImportLedgerFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun finishSignFlow() {
        back()
    }

    override fun openAddChainAccountSelectAddress(payload: AddLedgerChainAccountSelectAddressPayload) {
        navigationBuilder(R.id.action_addChainAccountSelectLedgerFragment_to_addChainAccountSelectAddressLedgerFragment)
            .setArgs(AddLedgerChainAccountSelectAddressFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSelectLedgerGeneric() {
        navigationBuilder(R.id.action_startImportGenericLedgerFragment_to_selectLedgerGenericImportFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectAddressGenericLedger(payload: SelectLedgerAddressPayload) {
        navigationBuilder(R.id.action_selectLedgerGenericImportFragment_to_selectAddressImportGenericLedgerFragment)
            .setArgs(SelectAddressLedgerFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openPreviewLedgerAccountsGeneric(payload: PreviewImportGenericLedgerPayload) {
        navigationBuilder(R.id.action_selectAddressImportGenericLedgerFragment_to_previewImportGenericLedgerFragment)
            .setArgs(PreviewImportGenericLedgerFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openFinishImportLedgerGeneric(payload: FinishImportGenericLedgerPayload) {
        navigationBuilder(R.id.action_previewImportGenericLedgerFragment_to_finishImportGenericLedgerFragment)
            .setArgs(FinishImportGenericLedgerFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }
}
