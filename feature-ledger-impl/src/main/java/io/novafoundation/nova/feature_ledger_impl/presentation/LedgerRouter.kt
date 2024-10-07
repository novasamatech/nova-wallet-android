package io.novafoundation.nova.feature_ledger_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerPayload

interface LedgerRouter : ReturnableRouter {

    fun openImportFillWallet()

    fun returnToImportFillWallet()

    fun openSelectImportAddress(payload: SelectLedgerAddressPayload)

    fun openCreatePincode()

    fun openMain()

    fun openFinishImportLedger(payload: FinishImportLedgerPayload)

    fun finishSignFlow()

    fun openAddChainAccountSelectAddress(payload: AddLedgerChainAccountSelectAddressPayload)

    // Generic app flows

    fun openSelectLedgerGeneric()

    fun openSelectAddressGenericLedger(payload: SelectLedgerAddressPayload)

    fun openPreviewLedgerAccountsGeneric(payload: PreviewImportGenericLedgerPayload)

    fun openFinishImportLedgerGeneric(payload: FinishImportGenericLedgerPayload)
}
