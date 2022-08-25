package io.novafoundation.nova.feature_ledger_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload

interface LedgerRouter : ReturnableRouter {

    fun openImportFillWallet()

    fun openImportSelectLedger(payload: SelectLedgerPayload)
}
