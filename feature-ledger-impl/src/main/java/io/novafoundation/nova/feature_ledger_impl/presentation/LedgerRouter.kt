package io.novafoundation.nova.feature_ledger_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.finish.FinishImportLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.SelectLedgerAddressPayload

interface LedgerRouter : ReturnableRouter {

    fun openImportFillWallet()

    fun returnToImportFillWallet()

    fun openSelectImportAddress(payload: SelectLedgerAddressPayload)

    fun openCreatePincode()

    fun openMain()

    fun openFinishImportLedger(payload: FinishImportLedgerPayload)
}
