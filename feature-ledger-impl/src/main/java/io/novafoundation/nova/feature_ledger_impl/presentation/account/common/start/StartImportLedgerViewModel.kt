package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter

abstract class StartImportLedgerViewModel(
    private val router: LedgerRouter,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun backClicked() {
        router.back()
    }

    abstract fun continueWithBluetooth()

    abstract fun continueWithUsb()

    fun guideClicked() {
        openBrowserEvent.value = appLinksProvider.ledgerConnectionGuide.event()
    }
}
