package io.novafoundation.nova.feature_ledger_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.SelectLedgerAddressPayload

interface LedgerRouter : ReturnableRouter {

    fun openImportFillWallet()

    fun openImportSelectLedger(payload: SelectLedgerPayload)

    fun openSelectImportAddress(payload: SelectLedgerAddressPayload)
}
