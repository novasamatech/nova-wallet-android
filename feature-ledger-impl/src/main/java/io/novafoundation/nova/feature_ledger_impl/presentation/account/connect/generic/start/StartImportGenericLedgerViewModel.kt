package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter

class StartImportGenericLedgerViewModel(
    private val router: LedgerRouter,
    private val appLinksProvider: AppLinksProvider,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        router.openSelectLedgerGeneric()
    }

    fun guideClicked() {
        openBrowserEvent.value = appLinksProvider.ledgerBluetoothGuide.event()
    }
}
