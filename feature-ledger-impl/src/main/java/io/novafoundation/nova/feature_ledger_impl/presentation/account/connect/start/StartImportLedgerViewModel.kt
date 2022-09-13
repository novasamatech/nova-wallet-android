package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class StartImportLedgerViewModel(
    private val router: LedgerRouter,
    private val appLinksProvider: AppLinksProvider,
    private val connectionRequirementsFlow: MutableSharedFlow<ChainConnection.ExternalRequirement>
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        launch { connectionRequirementsFlow.emit(ChainConnection.ExternalRequirement.ALLOWED) }
    }

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        router.openImportFillWallet()
    }

    fun guideClicked() {
        openBrowserEvent.value = appLinksProvider.ledgerBluetoothGuide.event()
    }
}
