package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start.StartImportLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.SelectLedgerGenericPayload

class StartImportGenericLedgerViewModel(
    private val router: LedgerRouter,
    private val appLinksProvider: AppLinksProvider,
) : StartImportLedgerViewModel(router, appLinksProvider), Browserable {

    override fun continueWithBluetooth() {
        router.openSelectLedgerGeneric(SelectLedgerGenericPayload(SelectLedgerPayload.ConnectionMode.BLUETOOTH))
    }

    override fun continueWithUsb() {
        router.openSelectLedgerGeneric(SelectLedgerGenericPayload(SelectLedgerPayload.ConnectionMode.USB))
    }
}
