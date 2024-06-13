package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import kotlinx.coroutines.flow.onStart

class StartImportLedgerViewModel(
    private val router: LedgerRouter,
    private val appLinksProvider: AppLinksProvider,
    private val ledgerMigrationTracker: LedgerMigrationTracker,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val shouldShowWarning = flowOf { ledgerMigrationTracker.anyChainSupportsMigrationApp() }
        .onStart { emit(false) }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        router.openImportFillWallet()
    }

    fun guideClicked() {
        openBrowserEvent.value = appLinksProvider.ledgerBluetoothGuide.event()
    }

    fun deprecationWarningClicked() {
        openBrowserEvent.value = appLinksProvider.ledgerMigrationArticle.event()
    }
}
